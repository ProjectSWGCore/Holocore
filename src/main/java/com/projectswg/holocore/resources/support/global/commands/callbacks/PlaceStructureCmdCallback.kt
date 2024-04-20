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
import com.projectswg.holocore.intents.gameplay.structures.PlaceStructureIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService
import kotlin.math.max
import kotlin.math.min

class PlaceStructureCmdCallback : ICmdCallback {
	
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val argumentsSplit = args.split(' ', limit=4)
		if (argumentsSplit.size != 4)
			return
		
		val creature = player.creatureObject ?: return
		
		try {
			val deed = ObjectStorageService.ObjectLookup.getObjectById(argumentsSplit[0].toLong()) as? TangibleObject
			if (deed == null) {
				StandardLog.onPlayerError(this, player, "Invalid deed ID in placestructure: %s", argumentsSplit[0])
				return
			}
			val terrain = creature.terrain
			val locationX = argumentsSplit[1].toDouble()
			val locationZ = argumentsSplit[2].toDouble()
			val direction = min(3, max(0, argumentsSplit[3].toInt()))
			val location = Location.builder()
				.setTerrain(terrain)
				.setX(locationX)
				.setY(ServerData.terrains.getHeight(terrain, locationX, locationZ))
				.setZ(locationZ)
				.setHeading(direction * 90.0)
				.build()
			
			PlaceStructureIntent(creature, deed, location).broadcast()
		} catch (e: NumberFormatException) {
			StandardLog.onPlayerError(this, player, "Invalid arguments to placestructure: %s", args)
		}
		StandardLog.onPlayerTrace(this, player, "Requested structure placement: %s", args)
	}
	
}