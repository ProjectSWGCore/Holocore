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
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject

import java.util.*
import java.util.concurrent.atomic.AtomicReference

class CreatureObjectAwareness(private val creature: CreatureObject) {
	
	private val aware = HashSet<SWGObject>()
	private val awareIds = HashSet<Long>()
	private val objectComparator = Comparator.comparingInt<SWGObject> { getObjectDepth(it) }.thenComparingDouble { this.getDistance(it) }
	private val finalTeleportPacket = AtomicReference<SWGPacket>(null)
	
	@Synchronized
	fun setTeleportDestination(parent: SWGObject?, location: Location) {
		if (parent == null)
			finalTeleportPacket.set(DataTransform(creature.objectId, 0, creature.nextUpdateCount, location, 0f))
		else
			finalTeleportPacket.set(DataTransformWithParent(creature.objectId, 0, creature.nextUpdateCount, parent.objectId, location, 0f))
	}
	
	@Synchronized
	fun flushNoPlayer() {
		val newAware = creature.aware
		val create = ArrayList<SWGObject>()
		val added = ArrayList<SWGObject>()
		
		// Create Deltas
		for (createCandidate in newAware) {
			if (aware.contains(createCandidate))
				continue
			added.add(createCandidate)
		}
		getCreateList(create, added)
		
		// Server awareness update
		for (add in create) { // Using "create" here because it's filtered to ensure no crashes
			aware.add(add)
			awareIds.add(add.objectId)
			add.addObserver(creature)
			creature.onObjectEnteredAware(add)
		}
		
		val destroy = ArrayList<SWGObject>()
		val removed = ArrayList<SWGObject>()
		
		// Create Deltas
		for (destroyCandidate in aware) {
			if (newAware.contains(destroyCandidate))
				continue
			removed.add(destroyCandidate)
		}
		getDestroyList(destroy, removed)
		
		// Remove destroyed objects so that nobody tries to send a packet to us after we send the destroy
		val it = aware.iterator()
		while (it.hasNext()) {
			val currentAware = it.next()
			for (remove in destroy) { // Since the "create" is filtered, aware could also have been filtered
				if (isParent(currentAware, remove)) {
					it.remove()
					awareIds.remove(currentAware.objectId)
					remove.removeObserver(creature)
					creature.onObjectExitedAware(remove)
					break
				}
			}
		}
	}
	
	@Synchronized
	fun flush(target: Player) {
		val newAware = creature.aware
		flushCreates(target, newAware)
		
		val finalTeleportPacket = this.finalTeleportPacket.getAndSet(null)
		if (finalTeleportPacket != null)
			creature.sendSelf(finalTeleportPacket)
		
		flushDestroys(target, newAware)
	}
	
	private fun flushCreates(target: Player, newAware: Set<SWGObject>) {
		val create = ArrayList<SWGObject>()
		val added = ArrayList<SWGObject>()
		
		// Create Deltas
		for (createCandidate in newAware) {
			if (aware.contains(createCandidate))
				continue
			added.add(createCandidate)
		}
		getCreateList(create, added)
		
		// Create the objects on the client
		val createStack = LinkedList<SWGObject>()
		for (obj in create) {
			if (isBundledObject(obj, obj.parent))
				popStackUntil(target, createStack, obj.parent)
			else
				popStackAll(target, createStack)
			createStack.add(obj)
			createObject(obj, target)
		}
		popStackAll(target, createStack)
		
		// Server awareness update
		for (add in create) { // Using "create" here because it's filtered to ensure no crashes
			aware.add(add)
			awareIds.add(add.objectId)
			add.addObserver(creature)
			creature.onObjectEnteredAware(add)
		}
		
		// Hope we didn't screw anything up
		assert(aware.contains(creature.getSlottedObject("ghost"))) { "not aware of ghost $creature" }
		assert(aware.contains(creature)) { "not aware of creature" }
	}
	
	private fun flushDestroys(target: Player, newAware: Set<SWGObject>) {
		val destroy = ArrayList<SWGObject>()
		val removed = ArrayList<SWGObject>()
		
		// Create Deltas
		for (destroyCandidate in aware) {
			if (newAware.contains(destroyCandidate))
				continue
			removed.add(destroyCandidate)
		}
		getDestroyList(destroy, removed)
		
		// Remove destroyed objects so that nobody tries to send a packet to us after we send the destroy
		val it = aware.iterator()
		while (it.hasNext()) {
			val currentAware = it.next()
			for (remove in destroy) { // Since the "create" is filtered, aware could also have been filtered
				if (isParent(currentAware, remove)) {
					it.remove()
					awareIds.remove(currentAware.objectId)
					remove.removeObserver(creature)
					creature.onObjectExitedAware(remove)
					break
				}
			}
		}
		
		// Destroy the objects on the client
		for (obj in destroy) {
			destroyObject(obj, target)
		}
		
		// Hope we didn't screw anything up
		assert(aware.contains(creature.getSlottedObject("ghost"))) { "not aware of ghost $creature" }
		assert(aware.contains(creature)) { "not aware of creature" }
	}
	
	@Synchronized
	fun resetObjectsAware() {
		for (obj in aware) {
			obj.removeObserver(creature)
		}
		aware.clear()
		awareIds.clear()
	}
	
	@Synchronized
	fun isAware(objectId: Long): Boolean {
		return awareIds.contains(objectId)
	}
	
	@Synchronized
	fun isAware(obj: SWGObject): Boolean {
		return aware.contains(obj)
	}
	
	private fun getCreateList(list: MutableList<SWGObject>, added: List<SWGObject>) {
		for (obj in added.sortedWith(objectComparator)) {
			val parent = obj.parent
			if (parent != null && !aware.contains(parent) && !list.contains(parent)) {
				assert(obj !is CellObject)
				continue
			}
			assert(obj !is BuildingObject || added.containsAll(obj.getContainedObjects())) { "All cells must be sent with the building" }
			if (!isBundledObject(obj, parent) || aware.contains(parent)) {
				list.add(obj)
			} else {
				val parentIndex = list.indexOf(parent)
				assert(parentIndex != -1) { "parent isn't added along with child" }
				list.add(parentIndex + 1, obj)
			}
		}
	}
	
	private fun getDestroyList(list: MutableList<SWGObject>, removed: List<SWGObject>) {
		for (obj in removed.sortedWith(objectComparator)) {
			// Don't delete our own parent nor child objects if we're deleting their parent (optimization)
			if (isParent(obj) || list.contains(obj.parent))
				continue
			list.add(obj)
		}
	}
	
	private fun isParent(obj: SWGObject): Boolean {
		return isParent(creature, obj)
	}
	
	private fun getDistance(obj: SWGObject): Double {
		return if (obj.parent != null) 0.0 else creature.worldLocation.distanceTo(obj.location)
		
	}
	
	private fun isBundledObject(obj: SWGObject, parent: SWGObject?): Boolean {
		return parent != null && (obj.slotArrangement == -1 || obj.baselineType == BaselineType.PLAY || parent === creature)
	}
	
	private fun createObject(obj: SWGObject, target: Player) {
		val id = obj.objectId
		run {
			// SceneCreateObjectByCrc
			val create = SceneCreateObjectByCrc()
			create.objectId = id
			create.location = obj.location
			create.objectCrc = obj.crc
			target.sendPacket(create)
		}
		run {
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
		}
		run {
			// Miscellaneous
			if (obj is CellObject)
				target.sendPacket(UpdateCellPermissionMessage(1.toByte(), id))
			// ? UpdatePostureMessage for PlayerObject ?
			if (obj is CreatureObject && obj.isGenerated()) {
				target.sendPacket(UpdatePostureMessage(obj.posture.id.toInt(), id))
				target.sendPacket(UpdatePvpStatusMessage(obj.pvpFaction, id, this.creature.getPvpFlagsFor(obj)))
			}
		}
		run {
			// UpdateContainmentMessage
			val parent = obj.parent
			if (parent != null)
				target.sendPacket(UpdateContainmentMessage(obj.objectId, parent.objectId, obj.slotArrangement))
		}
	}
	
	private fun popStackAll(target: Player, createStack: LinkedList<SWGObject>) {
		var parent: SWGObject? = createStack.pollLast()
		while (parent != null) {
			target.sendPacket(SceneEndBaselines(parent.objectId))
			parent = createStack.pollLast()
		}
	}
	
	private fun popStackUntil(target: Player, createStack: LinkedList<SWGObject>, parent: SWGObject?) {
		var last: SWGObject? = createStack.peekLast()
		while (last != null) {
			if (last === parent)
				break
			createStack.pollLast()
			target.sendPacket(SceneEndBaselines(last.objectId))
			last = createStack.peekLast()
		}
	}
	
	private fun destroyObject(obj: SWGObject, target: Player) {
		target.sendPacket(SceneDestroyObject(obj.objectId))
	}
	
	private fun getObjectDepth(obj: SWGObject?): Int {
		return if (obj == null) 0 else 1 + getObjectDepth(obj.parent)
	}
	
	private fun isParent(child: SWGObject, testParent: SWGObject): Boolean {
		var parent: SWGObject? = child
		while (parent != null) {
			if (parent === testParent)
				return true
			parent = parent.parent
		}
		return false
	}
	
}
