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
package services.player;

import java.sql.ResultSet;
import java.sql.SQLException;

import resources.Location;
import resources.Terrain;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class TerrainZoneInsertion {
	
	private final RelationalServerData insertions;
	
	public TerrainZoneInsertion() {
		insertions = RelationalServerFactory.getServerData("player/player_spawns.db", "building/buildings", "player_spawns");
		if (insertions == null)
			throw new main.ProjectSWG.CoreException("Failed to load SDBs for TerrainZoneInsertion");
	}
	
	public SpawnInformation generateSpawnLocation(String id) {
		final String whereClause = "(player_spawns.id = ?) AND (player_spawns.building_id = '' OR buildings.building_id = player_spawns.building_id)";
		try (ResultSet set = insertions.selectFromTable("player_spawns, buildings", new String[]{"player_spawns.*", "buildings.object_id"}, whereClause, id)) {
			if (!set.next())
				return null;
			String building = set.getString("building_id");
			Location l = generateRandomLocation(Terrain.getTerrainFromName(set.getString("terrain")), set.getDouble("x"), set.getDouble("y"), set.getDouble("z"), set.getDouble("radius"));
			return new SpawnInformation(!building.isEmpty(), l, set.getLong("object_id"), set.getString("cell"));
		} catch (SQLException e) {
			Log.e(e);
		}
		return null;
	}
	
	private static final Location generateRandomLocation(Terrain terrain, double x, double y, double z, double delta) {
		Location location = new Location();
		location.setTerrain(terrain);
		location.setX(x + (Math.random()-.5) * delta);
		location.setY(y);
		location.setZ(z + (Math.random()-.5) * delta);
		location.setOrientationX(0);
		location.setOrientationY(0);
		location.setOrientationZ(0);
		location.setOrientationW(1);
		return location;
	}
	
	public static class SpawnInformation {
		public final boolean building;
		public final Location location;
		public final long buildingId;
		public final String cell;
		
		public SpawnInformation(boolean building, Location location, long buildingId, String cell) {
			this.building = building;
			this.location = location;
			this.buildingId = buildingId;
			this.cell = cell;
		}
	}
	
}
