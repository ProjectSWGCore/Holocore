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
import resources.player.Player;
import services.galaxy.GalacticManager;

public class RequestWaypointCmdCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		// First parameter can be name of the planet or an int for the color
		// Args: (^-,=+_)color_1(,+-=_^)=1 planet x 0.0 z
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		String[] cmd = args.split(" ", 6);
		WaypointObject.WaypointColor color = WaypointObject.WaypointColor.BLUE;

/*		if (cmd[0].contains("color_")) {
			// Command is in color planet x y z name format
			String colorId = cmd[0].substring(14, cmd[0].length() - 10);
			color = WaypointObject.WaypointColor.values()[Integer.valueOf(colorId) - 1];
		}*/
		Terrain terrain = (color != null ? Terrain.getTerrainFromName(cmd[1]) : Terrain.getTerrainFromName(cmd[0]));

		double x = (color != null ? Double.valueOf(cmd[2]) : Double.valueOf(cmd[1]));
		double y = (color != null ? Double.valueOf(cmd[3]) : Double.valueOf(cmd[2]));
		double z = (color != null ? Double.valueOf(cmd[4]) : Double.valueOf(cmd[3]));

		String name = (cmd.length == 6 ? cmd[5] : "@planet_n:" + terrain.getName());

		WaypointObject waypoint = (WaypointObject) galacticManager.getObjectManager().createObject("object/waypoint/shared_waypoint.iff");
		waypoint.setLocation(new Location(x, y, z, terrain));
		waypoint.setName(name.isEmpty() ? "@planet_n:" + terrain.getName() : name);
		if (color != null)
			waypoint.setColor(color);
		ghost.addWaypoint(waypoint);
	}
}
