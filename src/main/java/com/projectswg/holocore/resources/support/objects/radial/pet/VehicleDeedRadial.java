/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.radial.pet;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.VehicleDeedGenerateIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

import java.util.Collection;

public class VehicleDeedRadial implements RadialHandlerInterface {
	
	public VehicleDeedRadial() {
		
	}
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		options.add(RadialOption.create(RadialItem.VEHICLE_GENERATE, "@pet/pet_menu:menu_generate"));
		options.add(RadialOption.create(RadialItem.ITEM_DESTROY));
		options.add(RadialOption.create(RadialItem.EXAMINE));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null)
			return;
		
		switch (selection) {
			case VEHICLE_GENERATE:
				VehicleDeedGenerateIntent.broadcast(creature, target);
				break;
			default:
				break;
		}
	}
	
}
