/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.support.data.dev

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.objectData
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox.Companion.getSelectedRow
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import com.projectswg.holocore.utilities.SdbGenerator
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2

class CustomObjectService : Service() {
	private val objects: MutableSet<SWGObject> = ConcurrentHashMap.newKeySet()
	private val createBox: ListBoxRecursive

	init {
		this.createBox = createListBoxRecursive()
	}

	@IntentHandler
	private fun handleExecuteCommandIntent(eci: ExecuteCommandIntent) {
		val command = eci.command
		if (!command.name.equals("object", ignoreCase = true)) return
		if (!isAuthorized(eci.source)) {
			broadcastPersonal(eci.source.owner!!, "You are not authorized to use this command")
			return
		}

		val target = eci.target ?: inferTarget(eci.source)
		val parts = eci.arguments.split(" ".toRegex(), limit = 3).toTypedArray()
		when (parts[0].lowercase()) {
			"create" -> if (parts.size < 2) handleCreate(eci.source)
			else handleCreate(eci.source, parts[1])

			"save"   -> handleSave(eci.source)
			"delete" -> handleDelete(eci.source, target)
			"move"   -> {
				if (parts.size < 3)
					broadcastPersonal(eci.source.owner!!, "move requires direction and amount")
				else
					handleMove(eci.source, target, parts[1], parts[2])
			}
			"rotate" -> {
				if (parts.size < 2)
					broadcastPersonal(eci.source.owner!!, "no rotation specified")
				else
					handleRotate(eci.source, target, parts[1])
			}
			"info"   -> {
				if (target == null) {
					broadcastPersonal(eci.source.owner!!, "No target")
				} else {
					broadcastPersonal(eci.source.owner!!, "Template: " + target.template)
					broadcastPersonal(eci.source.owner!!, "Location: " + target.location.position)
					broadcastPersonal(eci.source.owner!!, "Heading:  " + target.location.yaw)
				}
			}
			else     -> broadcastPersonal(eci.source.owner!!, "Unknown command: " + parts[0])
		}
	}

	private fun createListBoxRecursive(objects: Collection<String> = objectData().objects): ListBoxRecursive {
		val mapping: MutableMap<String, Any> = TreeMap()
		for (iff in objects) {
			mapping[prettyIff(iff)] = iff
		}
		return ListBoxRecursive(mapping)
	}

	private fun prettyIff(iff: String): String {
		val specific = iff.substring(iff.lastIndexOf('/') + 1).replace("shared_", "").replace(".iff", "")
		val folder = iff.substring(iff.lastIndexOf('/', iff.lastIndexOf('/') - 1) + 1, iff.lastIndexOf('/'))
		val parts = StringBuilder()
		for (part in specific.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
			if (part == folder || part.isEmpty()) continue
			if (parts.isNotEmpty()) parts.append(' ')
			parts.append(part.substring(0, 1).uppercase())
			parts.append(part.substring(1))
		}
		return parts.toString()
	}

	private fun handleCreate(source: CreatureObject) {
		createBox.display(source)
	}

	private fun handleCreate(source: CreatureObject, iff: String) {
		val obj = ObjectCreator.createObjectFromTemplate(iff)
		if (obj is BuildingObject) {
			obj.populateCells()
			for (child in obj.getContainedObjects()) ObjectCreatedIntent(child).broadcast()
		}
		obj.systemMove(source.parent, source.location)
		objects.add(obj)
		ObjectCreatedIntent(obj).broadcast()
	}

	private fun handleSave(source: CreatureObject) {
		try {
			SdbGenerator(File("custom_object_buildouts.sdb")).use { sdb ->
				var id: Long = 1
				sdb.writeColumnNames("buildout_id", "terrain", "template", "custom_name", "x", "y", "z", "heading", "building_name", "cell_id", "radius", "active", "comment")
				for (obj in objects) {
					sdb.writeLine(id, obj.terrain, obj.template, "", obj.x, obj.y, obj.z, obj.location.yaw, "", "0", "0", "TRUE", "")
					id++
				}
				broadcastPersonal(source.owner!!, "Saved " + (id - 1) + " objects to file")
			}
		} catch (e: IOException) {
			broadcastPersonal(source.owner!!, "Failed to save!")
			Log.e(e)
		}
	}

	private fun handleDelete(source: CreatureObject, target: SWGObject?) {
		if (target == null || !objects.remove(target)) {
			broadcastPersonal(source.owner!!, "Can't delete target: $target")
			return
		}
		DestroyObjectIntent(target).broadcast()
	}

	private fun handleMove(source: CreatureObject, target: SWGObject?, direction: String, amountStr: String) {
		if (target == null || !objects.contains(target)) {
			broadcastPersonal(source.owner!!, "Can't move target: $target")
			return
		}
		val amount: Double
		try {
			amount = amountStr.toDouble()
		} catch (e: NumberFormatException) {
			broadcastPersonal(source.owner!!, "Invalid amount")
			return
		}
		val location = when (direction.lowercase()) {
			"up"    -> Location.builder(target.location).translatePosition(0.0, amount, 0.0).build()
			"down"  -> Location.builder(target.location).translatePosition(0.0, -amount, 0.0).build()
			"north" -> Location.builder(target.location).translatePosition(0.0, 0.0, amount).build()
			"south" -> Location.builder(target.location).translatePosition(0.0, 0.0, -amount).build()
			"east"  -> Location.builder(target.location).translatePosition(amount, 0.0, 0.0).build()
			"west"  -> Location.builder(target.location).translatePosition(-amount, 0.0, 0.0).build()
			else    -> {
				broadcastPersonal(source.owner!!, "Unknown direction: $direction")
				return
			}
		}
		broadcastPersonal(source.owner!!, "Moved '" + target.template + "' " + amount + "m " + direction)
		target.moveToLocation(location)
	}

	private fun handleRotate(source: CreatureObject, target: SWGObject?, headingStr: String) {
		if (target == null || !objects.contains(target)) return
		try {
			val heading = headingStr.toDouble()
			target.moveToLocation(Location.builder(target.location).setHeading(heading).build())
			broadcastPersonal(source.owner!!, "Changed '" + target.template + "' heading to " + heading)
		} catch (e: NumberFormatException) {
			broadcastPersonal(source.owner!!, "Invalid heading")
		}
	}

	private fun inferTarget(source: CreatureObject): SWGObject? {
		if (source.lookAtTargetId != 0L) return ObjectLookup.getObjectById(source.lookAtTargetId)
		val world = source.worldLocation
		return source.objectsAware.stream().filter { o: SWGObject -> objects.contains(o) }.filter { tar: SWGObject -> abs(heading(world, tar.location) - world.orientation.heading) <= 30 }.min(Comparator.comparingDouble { tar: SWGObject -> world.flatDistanceTo(tar.location) }).orElse(null)
	}

	private inner class ListBoxRecursive(private val mappings: Map<String, Any>) {
		fun display(source: CreatureObject) {
			SuiListBox().run {
				title = "Custom Object - Create"
				prompt = "Select an item"

				for ((key, value) in mappings) {
					addListItem(key, value)
				}

				addOkButtonCallback("handle") { _: SuiEvent, parameters: Map<String, String> -> handle(source, getListItem(getSelectedRow(parameters)).obj) }
				display(source.owner ?: return)
			}
		}

		private fun handle(source: CreatureObject, obj: Any?) {
			when (obj) {
				is String           -> handleCreate(source, obj)
				is ListBoxRecursive -> obj.display(source)
				else                -> assert(false)
			}
		}
	}

	companion object {
		private fun isAuthorized(source: CreatureObject): Boolean {
			if (source.owner!!.accessLevel != AccessLevel.PLAYER) return true
			val groupId = source.groupId
			if (groupId != 0L) {
				val group = checkNotNull(ObjectLookup.getObjectById(groupId) as GroupObject?)
				for (groupMember in group.groupMemberObjects) {
					if (groupMember.owner!!.accessLevel != AccessLevel.PLAYER) return true
				}
			}
			return false
		}

		private fun heading(src: Location, dst: Location): Double {
			return (Math.toDegrees(atan2(dst.x - src.x, dst.z - src.z)) + 360) % 360
		}
	}
}
