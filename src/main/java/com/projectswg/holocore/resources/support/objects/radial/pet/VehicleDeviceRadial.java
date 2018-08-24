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
import com.projectswg.holocore.intents.gameplay.world.travel.pet.PetDeviceCallIntent;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.PetDeviceStoreIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;

import java.util.Collection;

public class VehicleDeviceRadial implements RadialHandlerInterface {
	
	public VehicleDeviceRadial() {
		
	}
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		if (!(target instanceof IntangibleObject))
			return;
		
		IntangibleObject pcd = (IntangibleObject) target;
		
		switch (pcd.getCount()) {
			case IntangibleObject.COUNT_PCD_STORED:
				options.add(RadialOption.create(RadialItem.VEHICLE_GENERATE));
				break;
			case IntangibleObject.COUNT_PCD_CALLED:
				options.add(RadialOption.create(RadialItem.VEHICLE_STORE));
				break;
		}
		
		options.add(RadialOption.create(RadialItem.ITEM_DESTROY));
		options.add(RadialOption.create(RadialItem.EXAMINE));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		if (!(target instanceof IntangibleObject) || selection == RadialItem.ITEM_USE)
			return;
		
		switch (selection) {
			case VEHICLE_GENERATE:
				PetDeviceCallIntent.broadcast(player, (IntangibleObject) target);
				break;
			case VEHICLE_STORE:
				PetDeviceStoreIntent.broadcast(player, (IntangibleObject) target);
				break;
			default:
				break;
		}
	}
	
}
