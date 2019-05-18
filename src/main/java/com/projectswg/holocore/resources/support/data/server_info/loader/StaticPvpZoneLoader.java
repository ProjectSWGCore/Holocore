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

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class StaticPvpZoneLoader extends DataLoader {
	
	private final Collection<StaticPvpZoneInfo> staticPvpZoneMap;
	
	public StaticPvpZoneLoader() {
		staticPvpZoneMap = new LinkedList<>();
	}
	
	public Collection<StaticPvpZoneInfo> getStaticPvpZones() {
		return Collections.unmodifiableCollection(staticPvpZoneMap);
	}
	
	@Override
	protected void load() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/gcw/pvp_zones.sdb"))) {
			while (set.next()) {
				StaticPvpZoneInfo staticPvpZone = new StaticPvpZoneInfo(set);
				
				staticPvpZoneMap.add(staticPvpZone);
			}
		}
	}
	
	public static class StaticPvpZoneInfo {
		private final int id;
		private final Location location;
		private final double radius;
		
		public StaticPvpZoneInfo(SdbLoader.SdbResultSet set) {
			id = (int) set.getInt("pvp_zone_id");
			
			location = Location.builder()
					.setX(set.getInt("x"))
					.setZ(set.getInt("z"))
					.setTerrain(Terrain.getTerrainFromName(set.getText("terrain")))
					.build();
			
			radius = set.getInt("radius");
		}
		
		public int getId() {
			return id;
		}
		
		public Location getLocation() {
			return location;
		}
		
		public double getRadius() {
			return radius;
		}
	}
}
