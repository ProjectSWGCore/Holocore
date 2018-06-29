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
package com.projectswg.holocore.resources.support.global.commands.callbacks;

import com.projectswg.holocore.intents.gameplay.entertainment.dance.DanceIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

public class StartDanceCallback implements ICmdCallback {
	
	private static final String ABILITY_NAME_PREFIX = "startDance+";
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		handleCommand(player, target, args, false);
	}
	
	protected void handleCommand(Player player, SWGObject target, String args, boolean changeDance) {
		CreatureObject creatureObject = player.getCreatureObject();
		
		// Not sure if args can ever actually be null. Better safe than sorry.
		if (args == null || args.isEmpty()) {
			// If no args are given, then bring up the SUI window for dance selection.
			SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "@performance:select_dance", "@performance:available_dances");
			Set<String> abilityNames = creatureObject.getAbilityNames();

			for (String abilityName : abilityNames) {
				if (abilityName.startsWith(ABILITY_NAME_PREFIX)) {
					String displayName = abilityName.replace(ABILITY_NAME_PREFIX, "");
					String firstCharacter = displayName.substring(0, 1);
					String otherCharacters = displayName.substring(1, displayName.length());

					listBox.addListItem(firstCharacter.toUpperCase(Locale.ENGLISH) + otherCharacters);
				}
			}

			listBox.addOkButtonCallback("handleSelectedItem", (event, parameters) -> {
				int selection = SuiListBox.getSelectedRow(parameters);
				String selectedDanceName = listBox.getListItem(selection).getName().toLowerCase(Locale.ENGLISH);
				
				new DanceIntent(selectedDanceName, player.getCreatureObject(), changeDance).broadcast();
			});
			
			listBox.display(player);
		} else {
			new DanceIntent(args, player.getCreatureObject(), changeDance).broadcast();
		}
	}
}
