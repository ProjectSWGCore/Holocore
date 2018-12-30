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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CmdSetExperience implements ICmdCallback {
	@Override
	public void execute(@NotNull Player player, @Nullable SWGObject target, @NotNull String args) {
		String[] argArray = args.split(" ");
		
		if (argArray.length != 2) {
			SystemMessageIntent.broadcastPersonal(player, "Expected format: /setExperience <xpType> <xpGained>");
			return;
		}
		
		String xpType = argArray[0];
		String xpGainedRaw = argArray[1];
		
		try {
			int xpGained = Integer.valueOf(xpGainedRaw);
			new ExperienceIntent(player.getCreatureObject(), xpType, xpGained).broadcast();
			
			Log.i("XP command: %s gave themselves %d %s XP", player.getUsername(), xpGained, xpType);
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(player, String.format("XP command: %s is not a number", xpGainedRaw));
			
			Log.e("XP command: %s gave a non-numerical XP gained argument of %s", player.getUsername(), xpGainedRaw);
		}
	}
}
