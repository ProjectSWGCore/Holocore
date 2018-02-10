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

import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Terrain;

import intents.chat.SystemMessageIntent;
import intents.object.ObjectCreatedIntent;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.SpecificObject;
import resources.objects.waypoint.WaypointObject;
import resources.player.Player;
import services.galaxy.GalacticManager;
import services.objects.ObjectCreator;

public class RequestWaypointCmdCallback implements ICmdCallback {
	
	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		String[] cmd = args.split(" ", 6);
		if (cmd.length < 5) {
			SystemMessageIntent.broadcastPersonal(player, "Invalid number of arguments for waypoint! Expected 5 or 6");
			return;
		}
		WaypointColor color = WaypointColor.BLUE;
		
		Terrain terrain = Terrain.getTerrainFromName(cmd[1]);
		
		Point3D position = new Point3D();
		position.set(Double.parseDouble(cmd[2]), Double.parseDouble(cmd[3]), Double.parseDouble(cmd[4]));
		
		String name = (cmd.length == 6 ? cmd[5] : "@planet_n:" + terrain.getName());
		
		createWaypoint(player, terrain, position, color, name);
	}
	
	private static void createWaypoint(Player player, Terrain terrain, Point3D position, WaypointColor color, String name) {
		WaypointObject waypoint = (WaypointObject) ObjectCreator.createObjectFromTemplate(SpecificObject.SO_WAYPOINT.getTemplate());
		waypoint.setPosition(terrain, position.getX(), position.getY(), position.getZ());
		waypoint.setName(name);
		waypoint.setColor(color);
		player.getPlayerObject().addWaypoint(waypoint);
		ObjectCreatedIntent.broadcast(waypoint);
	}
}
