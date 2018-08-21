/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/

package com.projectswg.holocore.resources.support.objects.radial.pet;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.DismountIntent;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.MountIntent;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.StoreMountIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.log.Log;

import java.util.List;

public class VehicleMountRadial implements RadialHandlerInterface {
	
	public VehicleMountRadial() {
		
	}
	
	@Override
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		if (!(target instanceof CreatureObject))
			return;
		
		CreatureObject mount = (CreatureObject) target;
		
		if (mount.getOwnerId() != player.getCreatureObject().getObjectId()) {	// Only an owner can enter/exit
			// TODO group members can enter the vehicle
			return;
		}
		
		if (player.getCreatureObject().getParent() == target)
			options.add(RadialOption.create(RadialItem.ITEM_USE, "@cmd_n:dismount"));
		else
			options.add(RadialOption.create(RadialItem.ITEM_USE, "@cmd_n:mount"));
		options.add(RadialOption.create(RadialItem.PET_STORE));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		if (!(target instanceof CreatureObject))
			return;
		switch (selection) {
			case SERVER_VEHICLE_ENTER_EXIT:
				if (player.getCreatureObject().getParent() == target)
					DismountIntent.broadcast(player, (CreatureObject) target);
				else
					MountIntent.broadcast(player, (CreatureObject) target);
				break;
			case PET_STORE:
				StoreMountIntent.broadcast(player, (CreatureObject) target);
				break;
			default:
				break;
		}
	}
	
}
