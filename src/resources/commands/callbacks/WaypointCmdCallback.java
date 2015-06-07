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

import resources.Location;
import resources.Terrain;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.waypoint.WaypointObject.WaypointColor;
import resources.player.Player;
import services.galaxy.GalacticManager;
import services.objects.ObjectManager;

public class WaypointCmdCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;
		
		String[] cmdArgs = args.split(" ");
		if (cmdArgs.length > 6)
			cmdArgs = args.split(" ", 6);

		WaypointColor color = null;
		Terrain terrain = null;
		String name = null;
		float x = -1;
		float y = -1;
		
		switch(cmdArgs.length) {
			case 2: // x y
				x = floatValue(cmdArgs[0]);
				if (x == -1)
					return;
				y = floatValue(cmdArgs[1]);
				break;
			case 4: // x z y name
				x = floatValue(cmdArgs[0]);
				if (x == -1)
					return;
				//z = floatValue(cmdArgs[1]);
				y = floatValue(cmdArgs[2]);
				name = cmdArgs[3];
				break;
			case 6: // planet x z y color name
				terrain = Terrain.getTerrainFromName(cmdArgs[0]);
				if (terrain == null)
					return;
				x = floatValue(cmdArgs[1]);
				if (x == -1)
					return;
				//z = floatValue(cmdArgs[2]);
				y = floatValue(cmdArgs[3]);
				color = colorValue(cmdArgs[4]);
				name = cmdArgs[5];
				break;
			default: 
				break;
		}
			WaypointObject waypoint = createWaypoint(galacticManager.getObjectManager(), terrain, color, name, x, y, player.getCreatureObject().getLocation());
			ghost.addWaypoint(waypoint);
		
	}

	private WaypointObject createWaypoint(ObjectManager objManager, Terrain terrain, WaypointColor color, String name, float x, float y, Location loc) {
		WaypointObject waypoint = (WaypointObject) objManager.createObject("object/waypoint/shared_waypoint.iff", false);

		waypoint.setLocation(new Location((x != -1 ? x : loc.getX()), 0, (y != -1 ? y : loc.getZ()), (terrain != null ? terrain : loc.getTerrain())));
		if (color != null)
			waypoint.setColor(color);

		waypoint.setName(name == null ? "New Waypoint" : name);
		
		return waypoint;
	}
	
	private void printCmdArgs(String[] args) {
		System.out.println("CmdArgs: ");
		for (String str : args) {
			System.out.print(str + ":");
		}
		System.out.println("");
	}
	
	private float floatValue(String str) {
		try {
			return Float.parseFloat(str);
		} catch (NumberFormatException | NullPointerException e) {
			return (float) -1;
		}
	}
	
	private WaypointColor colorValue(String str) {
		switch (str) {
			case "blue": return WaypointColor.BLUE;
			case "green": return WaypointColor.GREEN;
			case "yellow": return WaypointColor.YELLOW;
			case "white": return WaypointColor.WHITE;
			case "orange": return WaypointColor.ORANGE;
			case "purple": return WaypointColor.PURPLE;
			default: return WaypointColor.BLUE;
		}
	}
}
