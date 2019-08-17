/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class TravelCostLoader extends DataLoader {
	
	private final Map<Terrain, Map<Terrain, Integer>> costs;
	
	TravelCostLoader() {
		this.costs = new EnumMap<>(Terrain.class);
	}
	
	public boolean isCostDefined(Terrain source) {
		return costs.containsKey(source);
	}
	
	public int getCost(Terrain source, Terrain destination) {
		Map<Terrain, Integer> costMap = costs.get(source);
		if (costMap == null)
			return 0;
		return costMap.getOrDefault(destination, 0);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/travel/travel_costs.sdb"))) {
			while (set.next()) {
				TravelCostInfo travel = new TravelCostInfo(set);
				costs.put(travel.getPlanet(), travel.getCosts());
			}
		}
		for (Terrain key : costs.keySet()) {
			for (Map<Terrain, Integer> costMap : costs.values()) {
				assert costMap.keySet().equals(costs.keySet()) : "planet "+key+" is improperly defined in travel_costs.sdb";
			}
		}
	}
	
	public static class TravelCostInfo {
		
		private final Terrain planet;
		private final EnumMap<Terrain, Integer> costs;
		
		public TravelCostInfo(SdbResultSet set) {
			this.planet = Terrain.getTerrainFromName(set.getText("planet"));
			this.costs = new EnumMap<>(Terrain.class);
			for (String col : set.getColumns()) {
				if (col.equalsIgnoreCase("planet"))
					continue;
				costs.put(Terrain.getTerrainFromName(col), (int) set.getInt(col));
			}
		}
		
		public Terrain getPlanet() {
			return planet;
		}
		
		public Map<Terrain, Integer> getCosts() {
			return Collections.unmodifiableMap(costs);
		}
		
	}
}
