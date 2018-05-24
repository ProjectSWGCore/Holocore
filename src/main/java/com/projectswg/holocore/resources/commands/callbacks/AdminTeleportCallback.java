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
package com.projectswg.holocore.resources.commands.callbacks;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.object.ObjectTeleportIntent;
import com.projectswg.holocore.resources.commands.ICmdCallback;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.services.galaxy.GalacticManager;
import com.projectswg.holocore.services.player.CharacterLookupService.PlayerLookup;

public class AdminTeleportCallback implements ICmdCallback {

	@Override
	public void execute(Player player, SWGObject target, String args) {
		String [] cmd = args.split(" ");
		if (cmd.length < 4 || cmd.length > 5) {
			SystemMessageIntent.broadcastPersonal(player, "Wrong Syntax. For teleporting yourself, command has to be: /teleport <planetname> <x> <y> <z>");
			SystemMessageIntent.broadcastPersonal(player, "For teleporting another player, command has to be: /teleport <charname> <planetname> <x> <y> <z>");
			return;
		}
		double x, y, z;
		int cmdOffset = 0;
		if (cmd.length > 4)
			cmdOffset = 1; 
		try {
			x = Double.parseDouble(cmd[cmdOffset+1]);
			y = Double.parseDouble(cmd[cmdOffset+2]);
			z = Double.parseDouble(cmd[cmdOffset+3]);
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(player, "Wrong Syntax or Value. Please enter the command like this: /teleport <planetname> <x> <y> <z>");
			return;
		}
		
		Terrain terrain = Terrain.getTerrainFromName(cmd[cmdOffset]);
		if (terrain == null) {
			SystemMessageIntent.broadcastPersonal(player, "Wrong Syntax or Value. Invalid terrain: " + cmd[cmdOffset]);
			return;
		}
		
		CreatureObject teleportObject = player.getCreatureObject();
		if (cmd.length > 4) {
			teleportObject = PlayerLookup.getCharacterByFirstName(cmd[0]);
			if (teleportObject == null) {
				SystemMessageIntent.broadcastPersonal(player, "Invalid character first name: '"+cmd[0]+"'");
				return;
			}
		}
		
		ObjectTeleportIntent.broadcast(teleportObject, new Location(x, y, z, terrain));
	}

}
