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

package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TerrainZoneInsertionLoader extends DataLoader {
	
	private final Map<String, ZoneInsertion> zoneInsertions;
	
	TerrainZoneInsertionLoader() {
		this.zoneInsertions = new HashMap<>();
	}
	
	public ZoneInsertion getInsertion(String id) {
		return zoneInsertions.get(id);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/player/player_spawns.sdb"))) {
			while (set.next()) {
				ZoneInsertion def = new ZoneInsertion(set);
				zoneInsertions.put(def.getId(), def);
			}
		}
	}
	
	public static class ZoneInsertion {
		
		private final String id;
		private final Terrain terrain;
		private final String buildingId;
		private final String cell;
		private final double x;
		private final double y;
		private final double z;
		private final double radius;
		
		private ZoneInsertion(SdbResultSet set) {
			// id	terrain	building_id	cell	x	y	z	radius
			this.id = set.getText("id");
			this.terrain = Terrain.valueOf(set.getText("terrain").toUpperCase(Locale.US));
			this.buildingId = set.getText("building_id");
			this.cell = set.getText("cell");
			this.x = set.getReal("x");
			this.y = set.getReal("y");
			this.z = set.getReal("z");
			this.radius = set.getReal("radius");
		}
		
		public String getId() {
			return id;
		}
		
		public Terrain getTerrain() {
			return terrain;
		}
		
		public String getBuildingId() {
			return buildingId;
		}
		
		public String getCell() {
			return cell;
		}
		
		public double getX() {
			return x;
		}
		
		public double getY() {
			return y;
		}
		
		public double getZ() {
			return z;
		}
		
		public double getRadius() {
			return radius;
		}
		
		@Override
		public int hashCode() {
			return id.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof ZoneInsertion && ((ZoneInsertion) o).id.equals(id);
		}
		
	}
	
}
