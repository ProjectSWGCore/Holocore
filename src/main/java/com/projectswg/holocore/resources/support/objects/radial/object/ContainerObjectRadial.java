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
package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiInputBox;
import com.projectswg.holocore.resources.support.objects.permissions.ContainerPermissions;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

import java.util.Collection;
import java.util.Map;

public class ContainerObjectRadial extends SWGObjectRadial {
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		super.getOptions(options, player, target);

		// Only show the rename option if the requester has admin access to the targeted container
		ContainerPermissions containerPermissions = target.getContainerPermissions();
		CreatureObject creatureObject = player.getCreatureObject();
		
		if (containerPermissions.canMove(creatureObject, target)) {
			options.add(RadialOption.create(RadialItem.SET_NAME));
		}
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		ContainerPermissions containerPermissions = target.getContainerPermissions();
		CreatureObject creatureObject = player.getCreatureObject();
		
		if (!containerPermissions.canMove(creatureObject, target)) {
			return;
		}
		
		if (selection == RadialItem.SET_NAME) {
			SuiInputBox sui = new SuiInputBox(SuiButtons.OK_CANCEL, "@sui:set_name_title", "@sui:set_name_prompt");
			sui.addOkButtonCallback("rename_ok", (event, parameters) -> handleRename(parameters, target));
			sui.setPropertyText("txtInput", target.getObjectName());    // Insert the current name into the text fieldsui.display(player);
		}
	}
	
	private void handleRename(Map<String, String> parameters, SWGObject target) {
		String enteredText = SuiInputBox.getEnteredText(parameters);
		
		target.setObjectName(enteredText);
	}
	
}
