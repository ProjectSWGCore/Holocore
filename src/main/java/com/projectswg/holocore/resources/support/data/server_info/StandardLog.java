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
package com.projectswg.holocore.resources.support.data.server_info;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

public class StandardLog {
	
	public static long onStartLoad(String what) {
		Log.i("Loading %s...", what);
		return System.nanoTime();
	}
	
	public static void onEndLoad(int quantity, String what, long startTime) {
		Log.i("Finished loading %d %s. Took %.3fms", quantity, what, (System.nanoTime() - startTime) / 1E6);
	}
	
	public static void onPlayerTrace(@NotNull Object service, @NotNull CreatureObject player, @NotNull String event, Object ... args) {
		Log.t("[%s] %s %s", service.getClass().getSimpleName(), getInfo(player), String.format(event, args));
	}
	
	public static void onPlayerTrace(@NotNull Object service, @NotNull Player player, @NotNull String event, Object ... args) {
		Log.t("[%s] %s %s", service.getClass().getSimpleName(), getInfo(player), String.format(event, args));
	}
	
	public static void onPlayerEvent(@NotNull Object service, @NotNull CreatureObject player, @NotNull String event, Object ... args) {
		Log.d("[%s] %s %s", service.getClass().getSimpleName(), getInfo(player), String.format(event, args));
	}
	
	public static void onPlayerEvent(@NotNull Object service, @NotNull Player player, @NotNull String event, Object ... args) {
		Log.d("[%s] %s %s", service.getClass().getSimpleName(), getInfo(player), String.format(event, args));
	}
	
	public static void onPlayerError(@NotNull Object service, @NotNull CreatureObject player, @NotNull String event, Object ... args) {
		Log.e("[%s] %s %s", service.getClass().getSimpleName(), getInfo(player), String.format(event, args));
	}
	
	public static void onPlayerError(@NotNull Object service, @NotNull Player player, @NotNull String event, Object ... args) {
		Log.e("[%s] %s %s", service.getClass().getSimpleName(), getInfo(player), String.format(event, args));
	}
	
	private static String getInfo(CreatureObject creature) {
		Player player = creature.getOwner();
		if (player == null)
			return "NULL/"+creature.getObjectName();
		
		String username = player.getUsername();
		String characterName = creature.getObjectName();
		if (username.isEmpty() && characterName.isEmpty())
			return String.valueOf(player.getAddress());
		if (characterName.isEmpty())
			return username;
		return username + "/" + characterName;
	}
	
	private static String getInfo(Player player) {
		String username = player.getUsername();
		String characterName = player.getCharacterName();
		if (username.isEmpty() && characterName.isEmpty())
			return String.valueOf(player.getAddress());
		if (characterName.isEmpty())
			return username;
		return username + "/" + characterName;
	}
	
}
