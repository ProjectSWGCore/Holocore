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

package com.projectswg.holocore.resources.support.objects.radial.pet;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.world.DismountIntent;
import com.projectswg.holocore.intents.gameplay.world.MountIntent;
import com.projectswg.holocore.intents.gameplay.world.StoreMountIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class VehicleMountRadial implements RadialHandlerInterface {

	public VehicleMountRadial() {

	}

	@Override
	public void getOptions(@NotNull Collection<RadialOption> options, @NotNull Player player, @NotNull SWGObject target) {
		CreatureObject creature = player.getCreatureObject();
		if (!(target instanceof CreatureObject mount) || !isValidTarget(creature, (CreatureObject) target))
			return;

		options.add(RadialOption.create(RadialItem.EXAMINE));
		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT) && creature.getParent() == target)
			options.add(RadialOption.create(RadialItem.ITEM_USE, "@cmd_n:dismount"));
		else
			options.add(RadialOption.create(RadialItem.ITEM_USE, "@cmd_n:mount"));

		if (creature.getObjectId() == mount.getOwnerId())
			options.add(RadialOption.create(RadialItem.PET_STORE));
	}

	@Override
	public void handleSelection(@NotNull Player player, @NotNull SWGObject target, @NotNull RadialItem selection) {
		CreatureObject creature = player.getCreatureObject();
		if (!(target instanceof CreatureObject) || !isValidTarget(creature, (CreatureObject) target))
			return;

		switch (selection) {
			case ITEM_USE:
				if (player.getCreatureObject().getParent() == target)
					new DismountIntent(creature, (CreatureObject) target).broadcast();
				else
					new MountIntent(creature, (CreatureObject) target).broadcast();
				break;
			case PET_STORE:
				new StoreMountIntent(creature, (CreatureObject) target).broadcast();
				break;
			default:
				break;
		}
	}

	private static boolean isValidTarget(CreatureObject creature, CreatureObject mount) {
		// Owner of the vehicle
		if (creature.getObjectId() == mount.getOwnerId())
			return true;

		// Already mounted
		if (creature.getParent() == mount && mount.isStatesBitmask(CreatureState.MOUNTED_CREATURE) && creature.isStatesBitmask(CreatureState.RIDING_MOUNT))
			return true;

		// Within the same group
		GroupObject group = (GroupObject) ObjectLookup.getObjectById(creature.getGroupId());
		if (group == null || !group.getGroupMembers().containsValue(mount.getOwnerId())) {
			return false;
		}

		// Owner is already mounted
		return mount.getSlottedObject("rider") != null;
	}

}
