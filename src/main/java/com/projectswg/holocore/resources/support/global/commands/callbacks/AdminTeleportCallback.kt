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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Location.LocationBuilder
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.terrains
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup

class AdminTeleportCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val cmd = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		if (cmd.size < 2 || cmd.size > 5) {
			broadcastPersonal(player, "Wrong Syntax. For teleporting yourself, command has to be: /teleport <planetname> <x> <y> <z>")
			broadcastPersonal(player, "For teleporting another player, command has to be: /teleport <charname> <planetname> <x> <y> <z>")
			return
		}

		val creature = player.creatureObject ?: return
		val currentParent = creature.superParent
		val type = when (cmd.size) {
			3    -> TeleportType.PLANET_XZ
			4    -> if ((currentParent == null || isValidTerrain(cmd[0]))) TeleportType.PLANET_XYZ else TeleportType.CELL_XYZ
			5    -> TeleportType.OTHER_CHARACTER
			else -> TeleportType.XZ
		}
		val location = Location.builder()

		if (type == TeleportType.XZ || type == TeleportType.CELL_XYZ) {
			location.setTerrain(creature.terrain)
		} else {
			if (!parseTerrain(cmd, type, location)) {
				broadcastPersonal(player, "Wrong Syntax or Value. Invalid terrain: " + cmd[0])
				return
			}
		}

		if (!parseLocation(cmd, type, location)) {
			broadcastPersonal(player, "Wrong Syntax or Value. Please enter the command like this: /teleport <planetname> <x> <y> <z>")
			return
		}

		var newParent: SWGObject? = null
		if (type == TeleportType.CELL_XYZ) {
			if (currentParent is BuildingObject) {
				val cell: CellObject? = currentParent.getCellByName(cmd[0])
				if (cell == null) {
					broadcastPersonal(player, "Invalid cell name: " + cmd[0])
					return
				}
				newParent = cell
			} else {
				broadcastPersonal(player, "Invalid terrain or super parent: " + cmd[0])
				return
			}
		}

		var teleportObject = player.creatureObject
		if (type == TeleportType.OTHER_CHARACTER) {
			if (cmd[0].equals("group", ignoreCase = true)) {
				val group = ObjectLookup.getObjectById(teleportObject!!.groupId) as GroupObject?
				if (group != null) {
					for (member in group.groupMemberObjects) {
						member.moveToContainer(null, location.build())
					}
					return
				}
			}
			teleportObject = PlayerLookup.getCharacterByFirstName(cmd[0])
			if (teleportObject == null) {
				broadcastPersonal(player, "Invalid character first name: '" + cmd[0] + '\'')
				return
			}
		}

		teleportObject!!.moveToContainer(newParent, location.build())
	}

	private fun parseLocation(cmd: Array<String>, type: TeleportType, builder: LocationBuilder): Boolean {
		try {
			var startOffset = when (type) {
				TeleportType.PLANET_XZ, TeleportType.PLANET_XYZ, TeleportType.CELL_XYZ -> 1
				TeleportType.OTHER_CHARACTER                                           -> 2
				else                                                                   -> 0
			}
			builder.setX(cmd[startOffset++].toDouble())
			if (type == TeleportType.PLANET_XYZ || type == TeleportType.CELL_XYZ || type == TeleportType.OTHER_CHARACTER) builder.setY(cmd[startOffset++].toDouble())
			builder.setZ(cmd[startOffset].toDouble())
			if (type == TeleportType.XZ || type == TeleportType.PLANET_XZ) builder.setY(terrains.getHeight(builder))
			return true
		} catch (e: NumberFormatException) {
			return false
		}
	}

	private fun parseTerrain(cmd: Array<String>, type: TeleportType, builder: LocationBuilder): Boolean {
		try {
			builder.setTerrain(Terrain.valueOf(cmd[if (type == TeleportType.OTHER_CHARACTER) 1 else 0].uppercase()))
			return true
		} catch (e: IllegalArgumentException) {
			return false
		}
	}

	private fun isValidTerrain(terrainString: String): Boolean {
		try {
			Terrain.valueOf(terrainString.uppercase())
			return true
		} catch (e: IllegalArgumentException) {
			return false
		}
	}

	private enum class TeleportType {
		XZ,
		PLANET_XZ,
		PLANET_XYZ,
		CELL_XYZ,
		OTHER_CHARACTER
	}
}
