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

import com.projectswg.holocore.intents.gameplay.entertainment.dance.FlourishIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Mads
 */
public class FlourishCmdCallback implements ICmdCallback  {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		// No performance, no flourish!
		if (!player.getCreatureObject().isPerforming()) {
			new SystemMessageIntent(player, "@performance:flourish_not_performing").broadcast();
			return;
		}
		
		try {
			int flourishNumber = Integer.parseInt(args);

			if(flourishNumber < 1 || flourishNumber > 9) {
				new SystemMessageIntent(player, "@performance:flourish_not_valid").broadcast();
			} else if(flourishNumber == 9) {
				new FlourishIntent(player, "mistake").broadcast();
			} else {
				new FlourishIntent(player, "skill_action_" + flourishNumber).broadcast();
			}
		} catch (NumberFormatException e) {
			new SystemMessageIntent(player, "@performance:flourish_not_valid").broadcast();
		}


	}

}
