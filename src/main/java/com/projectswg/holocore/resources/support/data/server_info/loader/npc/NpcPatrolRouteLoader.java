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
package com.projectswg.holocore.resources.support.data.server_info.loader.npc;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class NpcPatrolRouteLoader extends DataLoader {
	
	private final Map<String, List<PatrolRouteWaypoint>> patrolRouteMap;
	
	public NpcPatrolRouteLoader() {
		this.patrolRouteMap = new HashMap<>();
	}
	
	public List<PatrolRouteWaypoint> getPatrolRoute(String groupId) {
		return patrolRouteMap.get(groupId);
	}
	
	public int getPatrolRouteCount() {
		return patrolRouteMap.size();
	}
	
	public void forEach(Consumer<List<PatrolRouteWaypoint>> c) {
		patrolRouteMap.values().forEach(c);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/patrol/patrol_id.msdb"))) {
			patrolRouteMap.putAll(set.stream(PatrolRouteWaypoint::new).collect(Collectors.groupingBy(PatrolRouteWaypoint::getGroupId)));
		}
	}
	
	public static class PatrolRouteWaypoint {
		
		private final String groupId;
		private final String patrolId;
		private final PatrolType patrolType;
		private final Terrain terrain;
		private final String buildingId;
		private final int cellId;
		private final double x;
		private final double y;
		private final double z;
		private final double delay;
		
		public PatrolRouteWaypoint(SdbResultSet set) {
			this.groupId = set.getText("patrol_group");
			this.patrolId = set.getText("patrol_id");
			this.patrolType = parsePatrolType(set.getText("patrol_type"));
			this.terrain = Terrain.valueOf(set.getText("terrain"));
			this.buildingId = set.getText("building_id");
			this.cellId = (int) set.getInt("cell_id");
			this.x = set.getReal("x");
			this.y = set.getReal("y");
			this.z = set.getReal("z");
			this.delay = set.getReal("pause");
		}
		
		public String getGroupId() {
			return groupId;
		}
		
		public String getPatrolId() {
			return patrolId;
		}
		
		public PatrolType getPatrolType() {
			return patrolType;
		}
		
		public Terrain getTerrain() {
			return terrain;
		}
		
		public String getBuildingId() {
			return buildingId;
		}
		
		public int getCellId() {
			return cellId;
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
		
		public double getDelay() {
			return delay;
		}
		
		private static PatrolType parsePatrolType(String str) {
			switch (str.toUpperCase(Locale.US)) {
				case "FLIP":
					return PatrolType.FLIP;
				case "LOOP":
				default:
					return PatrolType.LOOP;
			}
		}
		
	}
	
	public enum PatrolType {
		LOOP,
		FLIP
	}
	
}
