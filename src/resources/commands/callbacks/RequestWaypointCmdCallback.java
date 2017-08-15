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
import resources.objects.creature.CreatureObject;
import resources.objects.waypoint.WaypointObject;
import resources.player.Player;
import services.galaxy.GalacticManager;
import services.objects.ObjectCreator;

public class RequestWaypointCmdCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		String[] cmd = args.split(" ");
		CreatureObject creature = player.getCreatureObject();
		long cellId = 0;
		Point3D position = creature.getLocation().getPosition();
		Terrain terrain = creature.getTerrain();
		String expectedFormat = "/waypoint x y z";
		try {
			switch (cmd.length) {
				case 0:
					cellId = (creature.getParent() != null) ? creature.getParent().getObjectId() : 0;
					break;
				case 3: // x y z
					position.setX(Double.parseDouble(cmd[0]));
					position.setY(Double.parseDouble(cmd[1]));
					position.setZ(Double.parseDouble(cmd[2]));
					break;
				case 4: // terrain x y z
					expectedFormat = "/waypoint terrain x y z";
					terrain = Terrain.getTerrainFromName(cmd[0]);
					position.setX(Double.parseDouble(cmd[1]));
					position.setY(Double.parseDouble(cmd[2]));
					position.setZ(Double.parseDouble(cmd[3]));
					break;
				default:
					SystemMessageIntent.broadcastPersonal(player, "Warning: unknown number of args: "+cmd.length+" - defaulting to 0 argument call to waypoint");
					break;
			}
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(player, "Invalid call to waypoint [INVALID_NUMBER]! Expected: " + expectedFormat);
			return;
		}
		if (terrain == null) {
			SystemMessageIntent.broadcastPersonal(player, "Invalid call to waypoint [INVALID_TERRAIN]! Expected: " + expectedFormat);
			return;
		}
		createWaypoint(player, terrain, position, cellId);
	}
	
	private static void createWaypoint(Player player, Terrain terrain, Point3D position, long cellId) {
		WaypointObject waypoint = (WaypointObject) ObjectCreator.createObjectFromTemplate(SpecificObject.SO_WAYPOINT.getTemplate());
		waypoint.setPosition(terrain, position.getX(), position.getY(), position.getZ());
		waypoint.setCellId(cellId);
		waypoint.setName("@planet_n:" + terrain.getName());
		waypoint.setColor(WaypointColor.BLUE);
		player.getPlayerObject().addWaypoint(waypoint);
		ObjectCreatedIntent.broadcast(waypoint);
	}
}
