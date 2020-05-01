/***********************************************************************************
 * Copyright (c) 2020 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.services.support.global.admin

import com.projectswg.common.data.CRC
import com.projectswg.holocore.intents.support.global.chat.SystemChatRoomMessageIntent
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.chat.AdminChatRooms
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AdminSpawnerService : Service() {

	private val outputFile = File("log/spawners.txt")
	private val outputStreamLock = ReentrantLock()

	override fun start(): Boolean {
		outputFile.delete()
		outputFile.createNewFile()
		return true
	}

	@IntentHandler
	private fun handleChatCommandIntent(eci: ExecuteCommandIntent) {
		if (eci.command.crc == COMMAND_CREATE_SPAWNING_ELEMENT) {
			val creature = eci.source
			val location = creature.location
			val building = creature.superParent?.buildoutTag ?: location.terrain.name.toLowerCase(Locale.US).substring(0, 3) + "_world"
			val cell = (creature.parent as? CellObject?)?.number ?: 0
			val args = eci.arguments.split(' ', limit = 2)
			val type = sanitizeSpawnerType(args.getOrElse(0) { "AREA" }.toUpperCase(Locale.US))
			val comment = args.getOrElse(1) { "NPC" }
			val actualYaw = location.yaw
			val soeYaw = if (actualYaw < 180) actualYaw else actualYaw - 360
			val output = String.format("%s\t%s\t%s\t%d\t%.1f\t%.1f\t%.1f\t%.0f\t%s%s", location.terrain, type, building, cell, location.x, location.y, location.z, soeYaw, comment, System.lineSeparator())
			outputStreamLock.withLock {
				outputFile.appendText(output)
			}
			val color = when(type) {
				"PATROL" -> "#000000"
				"WAYPOINT" -> "#0000FF"
				else -> "#FFFF00"
			}
			val egg = ObjectCreator.createObjectFromTemplate(when(type) {
				"PATROL" -> SpawnerType.PATROL.objectTemplate
				"WAYPOINT" -> "object/tangible/ground_spawning/patrol_waypoint.iff"
				else -> SpawnerType.AREA.objectTemplate
			})
			egg.moveToContainer(creature.parent, location)
			ObjectCreatedIntent.broadcast(egg)
			SystemChatRoomMessageIntent.broadcast(AdminChatRooms.SPAWNER_LOG, "${color}The spawner $type ($comment) has been created at your location")
		}
	}
	
	companion object {
		
		private val COMMAND_CREATE_SPAWNING_ELEMENT = CRC.getCrc("createspawningelement")
		
		private fun sanitizeSpawnerType(type: String): String {
			if (type == "AREA" || type == "PATROL" || type == "WAYPOINT")
				return type
			return "AREA"
		}
		
	}

}
