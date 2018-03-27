/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.scripts.radial.object

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.intents.pet.PetCallIntent
import com.projectswg.holocore.intents.pet.PetMountIntent
import com.projectswg.holocore.resources.objects.SWGObject
import com.projectswg.holocore.resources.objects.creature.CreatureObject
import com.projectswg.holocore.resources.player.Player

class VehicleMountRadial extends SWGObjectRadial {
	
	@Override
	def getOptions(List<RadialOption> options, Player player, SWGObject target) {
		CreatureObject mount = (CreatureObject) target
		
		if (mount.getOwnerId() != player.getCreatureObject().getObjectId()) {	// Only an owner can enter/exit
			// TODO group members can enter the vehicle
			return
		}
		
		// TODO Store if called
		
		
		options.add(new RadialOption(RadialItem.SERVER_VEHICLE_ENTER))
		options.add(new RadialOption(RadialItem.SERVER_VEHICLE_EXIT))
		options.add(new RadialOption(RadialItem.PET_STORE))
	}
	
	@Override
	def handleSelection(Player player, SWGObject target, RadialItem selection) {
		switch (selection) {
			case RadialItem.SERVER_VEHICLE_ENTER:
				PetMountIntent.broadcast(player, (CreatureObject) target)	// TODO type check
				break
			case RadialItem.SERVER_VEHICLE_EXIT:
				break
			case RadialItem.PET_STORE:
				break
		}
	}
}
