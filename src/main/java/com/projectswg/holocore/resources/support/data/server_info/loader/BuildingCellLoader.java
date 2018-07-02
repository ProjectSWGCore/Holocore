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

import com.projectswg.common.data.location.Point3D;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class BuildingCellLoader extends DataLoader {
	
	private final Map<String, List<CellInfo>> buildingMap;
	
	BuildingCellLoader() {
		this.buildingMap = new HashMap<>();
	}
	
	@Nullable
	public List<CellInfo> getBuilding(String buildingIff) {
		return buildingMap.get(buildingIff);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/objects/building_cells.sdb"))) {
			while (set.next()) {
				buildingMap.computeIfAbsent(set.getText(0), b -> new ArrayList<>()).add(new CellInfo(set));
			}
		}
	}
	
	public static class CellInfo {
		
		private final int id;
		private final String name;
		private final Map<Point3D, Integer> neighbors;
		
		public CellInfo(SdbResultSet set) {
			this.id = (int) set.getInt(1);
			this.name = set.getText(2).intern();
			this.neighbors = new HashMap<>();
			for (String neighbor : set.getText(3).split(";")) {
				String [] neighborSplit = neighbor.split(",", 4);
				int neighborId = Integer.parseInt(neighborSplit[0]);
				neighbors.put(new Point3D(Double.parseDouble(neighborSplit[1]), Double.parseDouble(neighborSplit[2]), Double.parseDouble(neighborSplit[3])), neighborId);
			}
		}
		
		public String getName() {
			return name;
		}
		
		public int getId() {
			return id;
		}
		
		public Map<Point3D, Integer> getNeighbors() {
			return Collections.unmodifiableMap(neighbors);
		}
	}
	
}
