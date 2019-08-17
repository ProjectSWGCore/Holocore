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

import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.SpecificObject;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import org.jetbrains.annotations.NotNull;

public class RequestWaypointCmdCallback implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		String[] cmd = args.split(" ", 6);
		if (cmd.length < 5) {
			SystemMessageIntent.broadcastPersonal(player, "Invalid number of arguments for waypoint! Expected 5 or 6");
			return;
		}
		WaypointColor color = WaypointColor.BLUE;
		
		Terrain terrain = Terrain.getTerrainFromName(cmd[1]);
		
		Point3D position = new Point3D();
		position.set(Double.parseDouble(cmd[2]), Double.parseDouble(cmd[3]), Double.parseDouble(cmd[4]));
		
		String name = (cmd.length == 6 && !cmd[5].isBlank() ? cmd[5] : "@planet_n:" + terrain.getName());
		
		createWaypoint(player, terrain, position, color, name);
	}
	
	private static void createWaypoint(Player player, Terrain terrain, Point3D position, WaypointColor color, String name) {
		WaypointObject waypoint = (WaypointObject) ObjectCreator.createObjectFromTemplate(SpecificObject.SO_WAYPOINT.getTemplate());
		waypoint.setPosition(terrain, position.getX(), position.getY(), position.getZ());
		waypoint.setName(name);
		waypoint.setColor(color);
		if (!player.getPlayerObject().addWaypoint(waypoint))
			SystemMessageIntent.broadcastPersonal(player, "@base_player:too_many_waypoints");
		ObjectCreatedIntent.broadcast(waypoint);
	}
}
