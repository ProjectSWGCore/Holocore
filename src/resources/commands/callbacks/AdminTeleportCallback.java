/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package resources.commands.callbacks;

import intents.object.ObjectTeleportIntent;
import resources.Location;
import resources.Terrain;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.server_info.Log;
import services.galaxy.GalacticManager;

public class AdminTeleportCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		String [] cmd = args.split(" ");
		if (cmd.length < 4) {
			Log.e("Wrong Syntax. For teleporting yourself, command has to be: /teleport <planetname> <x> <y> <z>");
			Log.e("For teleporting another player, command has to be: /teleport <charname> <planetname> <x> <y> <z>");
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
			Log.e("Wrong Syntax or Value. Please enter the command like this: /teleport <planetname> <x> <y> <z>");
			return;
		}
		
		Location newLocation = new Location(x, y, z, Terrain.getTerrainFromName(cmd[cmdOffset]));
		SWGObject teleportObject = player.getCreatureObject();
		if (cmd.length > 4)
			teleportObject = galacticManager.getPlayerManager().getPlayerByCreatureFirstName(cmd[0]).getCreatureObject();
		new ObjectTeleportIntent(teleportObject, newLocation).broadcast();
	}

}
