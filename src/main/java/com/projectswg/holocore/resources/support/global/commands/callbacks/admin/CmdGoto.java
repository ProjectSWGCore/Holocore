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

import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectTeleportIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CmdGoto implements ICmdCallback  {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		SWGObject teleportee = player.getCreatureObject();
		if (teleportee == null)
			return;
		String [] parts = args.split(" ");
		if (parts.length == 0 || parts[0].trim().isEmpty())
			return;
		String loc = parts[0].trim();
		int cell = 1;
		try {
			if (parts.length >= 2)
				cell = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			
		}
		String err = teleportToGotoLocation(teleportee, loc, cell);
		new SystemMessageIntent(player, err).broadcast();
	}
	
	private String teleportToGotoLocation(SWGObject obj, String loc, int cell) {
		try (RelationalServerData data = RelationalServerFactory.getServerData("building/building.db", "buildings")) {
			try (ResultSet set = data.selectFromTable("buildings", null, "building_id = ?", loc)) {
				if (!set.next())
					return "No such location found: " + loc;
				Terrain t = Terrain.getTerrainFromName(set.getString("terrain_name"));
				return teleportToGoto(obj, set.getLong("object_id"), cell, new Location(0, 0, 0, t));
			} catch (SQLException e) {
				Log.e(e);
				return "Exception thrown. Failed to teleport: ["+e.getErrorCode()+"] " + e.getMessage();
			}
		}
	}
	
	private String teleportToGoto(SWGObject obj, long buildingId, int cellNumber, Location l) {
		SWGObject parent = ObjectLookup.getObjectById(buildingId);
		if (!(parent instanceof BuildingObject)) {
			String err = String.format("Invalid parent! Either null or not a building: %s  BUID: %d", parent, buildingId);
			Log.e(err);
			return err;
		}
		CellObject cell = ((BuildingObject) parent).getCellByNumber(cellNumber);
		if (cell == null) {
			String err = String.format("Building does not have any cells! B-Template: %s  BUID: %d", parent.getTemplate(), buildingId);
			Log.e(err);
			return err;
		}
		new ObjectTeleportIntent(obj, cell, l).broadcast();
		return "Successfully teleported "+obj.getObjectName()+" to "+buildingId;
	}
	
}
