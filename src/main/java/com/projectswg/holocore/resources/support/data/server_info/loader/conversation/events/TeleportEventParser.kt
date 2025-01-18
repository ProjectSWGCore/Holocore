/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader.conversation.events

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.gameplay.conversation.events.TeleportEvent
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.EventParser
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup

class TeleportEventParser : EventParser<TeleportEvent> {
	override fun parse(args: Map<String, Any>): TeleportEvent {
		val buildingName = args["building"] as String?
		val cellNumber = args["cell"] as Long?
		val terrainName = args["terrain"] as String? ?: throw IllegalArgumentException("Invalid terrain for teleport")
		val x = args["x"] as Double? ?: throw IllegalArgumentException("Invalid x-coordinate for teleport")
		val y = args["y"] as Double? ?: throw IllegalArgumentException("Invalid y-coordinate for teleport")
		val z = args["z"] as Double? ?: throw IllegalArgumentException("Invalid z-coordinate for teleport")
		val heading = args["heading"] as Double? ?: 0.0
		val radius = args["radius"] as Double? ?: 0.0
		
		val location = Location.builder()
			.setTerrain(Terrain.getTerrainFromName(terrainName) ?: throw IllegalArgumentException("Invalid terrain name"))
			.setX(x)
			.setY(y)
			.setZ(z)
			.setHeading(heading)
			.build()
		if (buildingName != null) {
			if (cellNumber == null)
				throw IllegalArgumentException("No cell number specified for building")
			
			val building = BuildingLookup.getBuildingByTag(buildingName) ?: throw IllegalArgumentException("Unknown building $buildingName")
			val cell = building.getCellByNumber(cellNumber.toInt()) ?: throw IllegalArgumentException("Invalid cell number for building $buildingName")
			
			return TeleportEvent(cell, location, radius)
		}

		return TeleportEvent(null, location, radius)
	}
}
