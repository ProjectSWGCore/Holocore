/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
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
import java.util.*
import kotlin.Comparator
import kotlin.NoSuchElementException

class CmdGoto : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val teleportee = player.creatureObject ?: return
		val parts = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		if (parts.size < 2 || parts.isEmpty() || parts[0].trim { it <= ' ' }.isEmpty() || parts[1].trim { it <= ' ' }.isEmpty()) return

		val type = parts[0].trim { it <= ' ' }
		val destination = parts[1].trim { it <= ' ' }

		var message = ""
		when (type) {
			"player"   -> message = (teleportToPlayer(player, teleportee, destination)) ?: return
			"building" -> message = teleportToBuilding(player, teleportee, destination, parts) ?: return
			"spawn"    -> message = teleportToSpawnId(player, teleportee, destination) ?: return
			"patrol"   -> message = teleportToPatrolId(player, teleportee, destination) ?: return
			else       -> broadcastPersonal(player, "Invalid goto command: $type")
		}

		broadcastPersonal(player, message)
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
			if (args.size >= 3) cell = args[2].toInt()
		} catch (e: NumberFormatException) {
			broadcastPersonal(player, "Invalid cell number")
			return null
		}
		return teleportToGoto(teleportee, building, cell)
	}

	private fun teleportToSpawnId(player: Player, teleportee: CreatureObject, spawnId: String): String? {
		val spawnInfo = try {
			ServerData.npcStaticSpawns.spawns.first { it.id == spawnId }
		} catch (e: NoSuchElementException) {
			broadcastPersonal(player, "Spawn ID '$spawnId' does not exist.")
			return null
		}
		val newLocation = Location.builder().setPosition(spawnInfo.x, spawnInfo.y, spawnInfo.z).setTerrain(spawnInfo.terrain).build()
		teleportToPoint(teleportee, spawnInfo.buildingId, spawnInfo.cellId, newLocation)
		return "Succesfully teleported " + teleportee.objectName + " to spawn " + spawnId
	}

	private fun teleportToPatrolId(player: Player, teleportee: CreatureObject, patrolId: String): String? {
		val groupIdFromPatrolId = patrolId.dropLast(2) + "00"
		val patrolPoint = try {
			Objects.requireNonNull(ServerData.npcPatrolRoutes[groupIdFromPatrolId], "Invalid patrol group ID: $groupIdFromPatrolId").first { it.patrolId == patrolId }
		} catch (e: NoSuchElementException) {
			broadcastPersonal(player, "Patrol ID '$patrolId' does not exist.")
			return null
		}
		val newLocation = Location.builder().setPosition(patrolPoint.x, patrolPoint.y, patrolPoint.z).setTerrain(patrolPoint.terrain).build()
		teleportToPoint(teleportee, patrolPoint.buildingId, patrolPoint.cellId, newLocation)
		return "Succesfully teleported " + teleportee.objectName + " to patrol " + patrolId
	}

	private fun teleportToPoint(teleportee: CreatureObject, buildingId: String, cellId: Int, newLocation: Location) {
		if (buildingId.isEmpty() || buildingId.endsWith("_world")) {
			if (teleportee.parent == null)
				teleportee.moveToLocation(newLocation)
			else
				teleportee.moveToContainer(null, newLocation)
			return
		}
		val newParent = BuildingLookup.getBuildingByTag(buildingId)?.getCellByNumber(cellId)
		teleportee.moveToContainer(newParent, newLocation)
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
