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
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Location.LocationBuilder;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import org.jetbrains.annotations.NotNull;

public class WaypointCmdCallback implements ICmdCallback {

	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		String[] cmdArgs = args.split(" ");

		WaypointColor color = WaypointColor.BLUE;
		Terrain terrain = null;
		String name = null;
		float x = Float.NaN;
		float z = Float.NaN;

		// Validate parameters, format for proper command arguments as it's split at whitespace
		for (int i = 0; i < cmdArgs.length; i++) {
			switch(i) {
				// This could be either for just a named waypoint at current spot (1 param) or a planet arg (6 param)
				case 0:
					if (Terrain.getTerrainFromName(cmdArgs[0]) != null) {
						// Terrain's name could also be part of the waypoint name, check to see if next few args are coords
						try {
							if (cmdArgs.length > 2) {
								x = Float.parseFloat(cmdArgs[1]);
								z = Float.parseFloat(cmdArgs[2]); // Just to be sure.. Maybe someone wanted some numbers in the name.
								if (cmdArgs.length != 6)
									cmdArgs = args.split(" ", 6);
							}
						} catch (NumberFormatException e) {
							// This is just a named waypoint.
							cmdArgs = new String[]{args};
						}
					} else {
						// This is just a named waypoint.
						cmdArgs = new String[]{args};
					}
					break;
				// This could be either for a name (3 param) or a z coordinate (6 param)
				case 3:
					try {
						z = Float.parseFloat(cmdArgs[3]);
						// Ensure 100% this is a 6 argument command as the first param MUST be the planet name
						if (Terrain.getTerrainFromName(cmdArgs[0]) != null)
							if (cmdArgs.length != 6)
								cmdArgs = args.split(" ", 6);
						else cmdArgs = args.split(" ", 4);
					} catch (NumberFormatException e) {
						// This is intended for a name, should be 4 params
						cmdArgs = args.split(" ", 4);
					}
					break;
				default: break;
			}
		}

		// Was there an error message saying the format was wrong?
		
		switch(cmdArgs.length) {
			case 1: // name
				name = cmdArgs[0];
				break;
			case 2: // x y
				x = floatValue(cmdArgs[0]);
				if (Float.isNaN(x))
					break;
				z = floatValue(cmdArgs[1]);
				break;
			case 3: // x y z
				x = floatValue(cmdArgs[0]);
				if (Float.isNaN(x))
					break;
				//y = floatValue(cmdArgs[1]);
				z = floatValue(cmdArgs[2]);
				break;
			case 4: // x y z name
				x = floatValue(cmdArgs[0]);
				if (Float.isNaN(x))
					break;
				//y = floatValue(cmdArgs[1]);
				z = floatValue(cmdArgs[2]);
				if (Float.isNaN(z))
					break;
				name = cmdArgs[3];
				break;
			case 6: // planet x y z color name
				terrain = Terrain.getTerrainFromName(cmdArgs[0]);
				if (terrain == null)
					break;
				x = floatValue(cmdArgs[1]);
				if (Float.isNaN(x))
					break;
				//y = floatValue(cmdArgs[2]);
				z = floatValue(cmdArgs[3]);
				if (Float.isNaN(z))
					break;
				color = WaypointColor.fromString(cmdArgs[4]);
				name = cmdArgs[5];
				break;
			default:
				// Not a valid format for /waypoint command
				return;
		}

		CreatureObject creature = player.getCreatureObject();
		LocationBuilder location = Location.builder(creature.getWorldLocation());

		if (!Float.isNaN(x))
			location.setX(x);

		if (!Float.isNaN(z))
			location.setZ(z);

		boolean differentPlanetMessage = false;
		if (terrain != null) {
			if (terrain != location.getTerrain()) {
				location.setTerrain(terrain);
				differentPlanetMessage = true;
			}
		}

		if (name == null || name.isEmpty())
			name = "Waypoint";

		ghost.addWaypoint(createWaypoint(color, name, location.build()));

		if (differentPlanetMessage) {
			new SystemMessageIntent(player, "Waypoint: New waypoint \""+ name + "\" created for location "
					+ terrain.getName() + " (" + String.format("%.0f", location.getX()) + ", "
					+ String.format("%.0f", location.getY()) + ", "+ String.format("%.0f", location.getZ()) + ")").broadcast();
		} else {
			new SystemMessageIntent(player, "Waypoint: New waypoint \""+ name + "\" created for location ("
					+ String.format("%.0f", location.getX()) + ", "+ String.format("%.0f", location.getY())
					+ ", "+ String.format("%.0f", location.getZ()) + ")").broadcast();
		}
	}

	private WaypointObject createWaypoint(WaypointColor color, String name, Location location) {
		WaypointObject waypoint = (WaypointObject) ObjectCreator.createObjectFromTemplate("object/waypoint/shared_waypoint.iff");
		waypoint.setPosition(location.getTerrain(), location.getX(), location.getY(), location.getZ());
		waypoint.setColor(color);
		waypoint.setName(name);
		new ObjectCreatedIntent(waypoint).broadcast();
		return waypoint;
	}
	
	private float floatValue(String str) {
		try {
			return Float.parseFloat(str);
		} catch (NumberFormatException | NullPointerException e) {
			return Float.NaN;
		}
	}
}
