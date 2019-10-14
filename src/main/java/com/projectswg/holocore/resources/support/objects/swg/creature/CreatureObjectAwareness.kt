/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */

package com.projectswg.holocore.resources.support.objects.swg.creature

import com.projectswg.common.data.location.Location
import com.projectswg.common.network.packets.SWGPacket
import com.projectswg.common.network.packets.swg.zone.*
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.common.network.packets.swg.zone.building.UpdateCellPermissionMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class CreatureObjectAwareness(private val creature: CreatureObject) {
	
	private val aware = HashSet<SWGObject>()
	private val awareIds = HashSet<Long>()
	private val finalTeleportPacket = AtomicReference<SWGPacket>(null)
	private val flushAwarenessData = FlushAwarenessData(creature)
	
	@Synchronized
	fun setTeleportDestination(parent: SWGObject?, location: Location) {
		if (parent == null)
			finalTeleportPacket.set(DataTransform(creature.objectId, 0, creature.nextUpdateCount, location, 0f))
		else
			finalTeleportPacket.set(DataTransformWithParent(creature.objectId, 0, creature.nextUpdateCount, parent.objectId, location, 0f))
	}
	
	@Synchronized
	fun flushNoPlayer() {
		handleFlush({}, {}, {}) // No action required for NPCs
	}
	
	@Synchronized
	fun flush(target: Player) {
		handleFlush(
			createHandler = { create ->
				// Send all SceneCreateObject's and Baselines
				val createStack = LinkedList<SWGObject>()
				for (obj in create) {
					val parent = obj.parent
					if (parent != null) {
						popStackUntil(target, createStack, parent)
						if (!obj.isBundledWithin(parent, creature) && createStack.peekLast() == parent) {
							target.sendPacket(SceneEndBaselines(parent.objectId))
							createStack.pollLast()
						}
					} else {
						popStackAll(target, createStack)
					}
					createStack.add(obj)
					createObject(obj, target)
				}
				popStackAll(target, createStack)
			},
			intermediateCallback = {
				// Perform a teleport if necessary
				val finalTeleportPacket = this.finalTeleportPacket.getAndSet(null)
				if (finalTeleportPacket != null)
					creature.sendSelf(finalTeleportPacket)
			},
			destroyHandler = { destroy ->
				// Destroy the objects on the client
				for (obj in destroy) {
					destroyObject(obj, target)
				}
			})
	}
	
	private inline fun handleFlush(createHandler: (Collection<SWGObject>) -> Unit, intermediateCallback: () -> Unit, destroyHandler: (Collection<SWGObject>) -> Unit) {
		val newAware = creature.aware
		handleFlushCreate(newAware, createHandler)
		intermediateCallback()
		handleFlushDestroy(newAware, destroyHandler)
	}
	
	private inline fun handleFlushCreate(newAware: Set<SWGObject>, createHandler: (Collection<SWGObject>) -> Unit) {
		val create = flushAwarenessData.buildCreate(aware, newAware)
		createHandler(create)
		
		for (add in create) { // Using "create" here because it's filtered to ensure no crashes
			aware.add(add)
			awareIds.add(add.objectId)
			add.addObserver(creature)
			creature.onObjectEnteredAware(add)
		}
		assert(!creature.isLoggedInPlayer || aware.contains(creature.getSlottedObject("ghost"))) { "not aware of ghost $creature" }
		assert(!creature.isLoggedInPlayer || aware.contains(creature)) { "not aware of creature" }
	}
	
	private inline fun handleFlushDestroy(newAware: Set<SWGObject>, destroyHandler: (Collection<SWGObject>) -> Unit) {
		val destroy = flushAwarenessData.buildDestroy(aware, newAware)
		// Remove destroyed objects immediately - before sending anything to the client
		val it = aware.iterator()
		while (it.hasNext()) {
			val currentAware = it.next()
			for (remove in destroy) { // Since the "create" is filtered, aware could also have been filtered
				if (currentAware === remove || isParent(currentAware, remove)) {
					it.remove()
					awareIds.remove(currentAware.objectId)
					remove.removeObserver(creature)
					creature.onObjectExitedAware(remove)
					break
				}
			}
		}
		destroyHandler(destroy)
		assert(!creature.isLoggedInPlayer || aware.contains(creature.getSlottedObject("ghost"))) { "not aware of ghost $creature" }
		assert(!creature.isLoggedInPlayer || aware.contains(creature)) { "not aware of creature" }
	}
	
	@Synchronized
	fun resetObjectsAware() {
		for (obj in aware) {
			obj.removeObserver(creature)
		}
		aware.clear()
		awareIds.clear()
	}
	
	@Synchronized fun isAware(objectId: Long) = awareIds.contains(objectId)
	@Synchronized fun isAware(obj: SWGObject) = aware.contains(obj)
	
	private fun createObject(obj: SWGObject, target: Player) {
		val id = obj.objectId
		
		// SceneCreateObjectByCrc
		val create = SceneCreateObjectByCrc()
		create.objectId = id
		create.location = obj.location
		create.objectCrc = obj.crc
		target.sendPacket(create)
		
		// Baselines
		val owner = obj.owner === target
		
		target.sendPacket(obj.createBaseline3(target))
		target.sendPacket(obj.createBaseline6(target))
		
		if (owner) {
			target.sendPacket(obj.createBaseline1(target))
			target.sendPacket(obj.createBaseline4(target))
			target.sendPacket(obj.createBaseline8(target))
			target.sendPacket(obj.createBaseline9(target))
		}
		
		// Miscellaneous
		if (obj is CellObject)
			target.sendPacket(UpdateCellPermissionMessage(1.toByte(), id))
		// ? UpdatePostureMessage for PlayerObject ?
		if (obj is CreatureObject && obj.isGenerated()) {
			target.sendPacket(UpdatePostureMessage(obj.posture.id.toInt(), id))
			target.sendPacket(UpdatePvpStatusMessage(obj.pvpFaction, id, this.creature.getPvpFlagsFor(obj)))
		}
		
		// UpdateContainmentMessage
		val parent = obj.parent
		if (parent != null)
			target.sendPacket(UpdateContainmentMessage(obj.objectId, parent.objectId, obj.slotArrangement))
	}
	
	private fun popStackAll(target: Player, createStack: LinkedList<SWGObject>) {
		var parent: SWGObject? = createStack.pollLast()
		while (parent != null) {
			target.sendPacket(SceneEndBaselines(parent.objectId))
			parent = createStack.pollLast()
		}
	}
	
	private fun popStackUntil(target: Player, createStack: LinkedList<SWGObject>, parent: SWGObject) {
		var last: SWGObject? = createStack.peekLast()
		val grandparent = parent.parent
		while (last != null && !(last === parent) && !(last === grandparent)) {
			createStack.pollLast()
			target.sendPacket(SceneEndBaselines(last.objectId))
			last = createStack.peekLast()
		}
	}
	
	private fun destroyObject(obj: SWGObject, target: Player) {
		target.sendPacket(SceneDestroyObject(obj.objectId))
	}
	
	class FlushAwarenessData(private val creature: CreatureObject) {
		
		private val create: MutableCollection<SWGObject> = if (creature is AIObject) ArrayList() else TreeSet(kotlin.Comparator(::compare))
		private val destroy: MutableCollection<SWGObject> = ArrayList()
		
		fun buildCreate(oldAware: Set<SWGObject>, newAware: Set<SWGObject>): Collection<SWGObject> {
			val create = this.create
			create.clear()
			for (obj in newAware) {
				if (!oldAware.contains(obj)) {
					create.add(obj)
				}
			}
			return create
		}
		
		fun buildDestroy(oldAware: Set<SWGObject>, newAware: Set<SWGObject>): Collection<SWGObject> {
			val destroy = this.destroy
			destroy.clear()
			oldAware.asSequence()
					.filter { !newAware.contains(it) }       // Removed from old to new awareness
					.filter { !isParent(creature, it) }      // Only remove if it isn't our parent
					.sortedBy { getObjectDepth(it) }            // Sorted for proper ordering
					.forEach {
						var parent = it.parent
						while (parent != null) {
							if (destroy.contains(parent))
								return@forEach               // Optimization for the client - destroying the parent destroys the children
							parent = parent.parent
						}
						destroy.add(it)
					}
			
			return destroy
		}
		
		private fun compare(a: SWGObject, b: SWGObject): Int {
			var tmpA: SWGObject = a
			var tmpAP: SWGObject? = a.parent
			while (tmpAP != null) {
				if (tmpAP === b)           // A is a child of B
					return 1
				var tmpB: SWGObject = b
				var tmpBP: SWGObject? = b.parent
				while (tmpBP != null) {
					if (tmpBP === tmpA)    // B is a child of A
						return -1
					if (tmpAP === tmpBP) { // Found a shared parent, compare their next highest children
						val bundledA = tmpA.isBundledWithin(tmpAP, creature)
						val bundledB = tmpB.isBundledWithin(tmpBP, creature)
						if (bundledA && !bundledB)
							return -1
						if (bundledB && !bundledA)
							return 1
						return tmpA.objectId.compareTo(tmpB.objectId) // If they're both bundled/not bundled, doesn't matter what order they're in
					}
					tmpB = tmpBP
					tmpBP = tmpB.parent
				}
				tmpA = tmpAP
				tmpAP = tmpA.parent
				if (tmpAP == null) {
					val distComp = getDistance(creature, tmpA).compareTo(getDistance(creature, tmpB))
					if (distComp != 0)
						return distComp
					return tmpA.objectId.compareTo(tmpB.objectId)
				}
			}
			// A is a top level object, since the above loop did not run
			var tmpB: SWGObject = b
			var tmpBP: SWGObject? = b.parent
			while (tmpBP != null) {
				tmpB = tmpBP
				tmpBP = tmpB.parent
			}
			if (tmpA === tmpB)
				return -1 // B is a child of A
			val distComp = getDistance(creature, tmpA).compareTo(getDistance(creature, tmpB))
			if (distComp != 0)
				return distComp
			return tmpA.objectId.compareTo(tmpB.objectId)
		}
		
	}
	
	companion object {
		
		private fun getDistance(creature: CreatureObject, obj: SWGObject) = if (obj.parent != null) 0 else creature.worldLocation.distanceTo(obj.location).toInt()
		private fun SWGObject.isBundledWithin(parent: SWGObject, creature: CreatureObject) = (this.slotArrangement == -1 || this.baselineType == BaselineType.PLAY || parent === creature)
		
		private fun getObjectDepth(obj: SWGObject?): Int {
			var depth = 0
			var tmp = obj
			while (tmp != null) {
				depth++
				tmp = tmp.parent
			}
			return depth
		}
		
		private fun isParent(child: SWGObject, testParent: SWGObject): Boolean {
			var parent: SWGObject? = child.parent
			while (parent != null) {
				if (parent === testParent)
					return true
				parent = parent.parent
			}
			return false
		}
		
	}
	
}
