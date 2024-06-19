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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup
import me.joshlarson.jlcommon.log.Log

class CmdGoto : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val teleportee = player.creatureObject ?: return
		val parts = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		if (parts.isEmpty() || parts[0].trim { it <= ' ' }.isEmpty()) return

		val destination = parts[0].trim { it <= ' ' }
		val message = if (PlayerLookup.doesCharacterExistByFirstName(destination)) teleportToPlayer(player, teleportee, destination) else teleportToBuilding(player, teleportee, destination, parts)

		if (message != null) broadcastPersonal(player, message)
	}

	private fun teleportToPlayer(player: Player, teleportee: CreatureObject, playerName: String): String? {
		val destinationPlayer = PlayerLookup.getCharacterByFirstName(playerName)
		if (destinationPlayer == null) {
			broadcastPersonal(player, "Unknown player: $playerName")
			return null
		}

		if (destinationPlayer.isStatesBitmask(CreatureState.RIDING_MOUNT)) teleportee.moveToContainer(null, destinationPlayer.worldLocation)
		else teleportee.moveToContainer(destinationPlayer.parent, destinationPlayer.location)

		return "Successfully teleported " + teleportee.objectName + " to " + destinationPlayer.objectName
	}

	private fun teleportToBuilding(player: Player, teleportee: CreatureObject, buildingName: String, args: Array<String>): String? {
		val building = BuildingLookup.getBuildingByTag(buildingName)
		if (building == null) {
			broadcastPersonal(player, "Unknown building: $buildingName")
			return null
		}
		var cell = 1
		try {
			if (args.size >= 2) cell = args[1].toInt()
		} catch (e: NumberFormatException) {
			broadcastPersonal(player, "Invalid cell number")
			return null
		}
		return teleportToGoto(teleportee, building, cell)
	}

	private fun teleportToGoto(obj: SWGObject, building: BuildingObject, cellNumber: Int): String {
		val cell = building.getCellByNumber(cellNumber)
		if (cell == null) {
			val err = String.format("Building '%s' does not have cell %d", building, cellNumber)
			Log.e(err)
			return err
		}
		val portal = cell.getPortals().stream().min(Comparator.comparingInt { p: Portal -> if ((p.getOtherCell(cell) == null)) 0 else p.getOtherCell(cell)!!.number }).orElse(null)

		var x = 0.0
		var y = 0.0
		var z = 0.0
		if (portal != null) {
			x = (portal.frame1.x + portal.frame2.x) / 2
			y = (portal.frame1.y + portal.frame2.y) / 2
			z = (portal.frame1.z + portal.frame2.z) / 2
		}
		obj.moveToContainer(cell, Location.builder().setPosition(x, y, z).setTerrain(building.terrain).build())
		return "Successfully teleported " + obj.objectName + " to " + building.buildoutTag
	}
}
