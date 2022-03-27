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
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.json.JSON;
import me.joshlarson.json.JSONArray;
import me.joshlarson.json.JSONException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

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
			buildingMap.putAll(set.stream(CellInfo::new).collect(groupingBy(CellInfo::getBuilding)));
		}
	}
	
	public static class CellInfo {
		
		private final String building;
		private final int id;
		private final String name;
		private final List<PortalInfo> neighbors;
		
		public CellInfo(SdbResultSet set) {
			this.building = set.getText(0);
			this.id = (int) set.getInt(1);
			this.name = set.getText(2).intern();
			this.neighbors = new ArrayList<>();
			try {
				JSONArray parts = JSON.readArray(set.getText(3));
				for (int i = 0; i < parts.size(); i++) {
					JSONArray neighbor = new JSONArray(parts.getArray(i));
					JSONArray min = new JSONArray(neighbor.getArray(1));
					JSONArray max = new JSONArray(neighbor.getArray(2));
					Point3D p1 = new Point3D(min.getDouble(0), min.getDouble(1), min.getDouble(2));
					Point3D p2 = new Point3D(max.getDouble(0), min.getDouble(1), max.getDouble(2));
					neighbors.add(new PortalInfo(id, neighbor.getInt(0), p1, p2, max.getDouble(1) - min.getDouble(1)));
				}
			} catch (IOException | JSONException e) {
				Log.w("Invalid cell info: %s", set.getText(3));
			}
		}
		
		public String getBuilding() {
			return building;
		}
		
		public String getName() {
			return name;
		}
		
		public int getId() {
			return id;
		}
		
		public List<PortalInfo> getNeighbors() {
			return Collections.unmodifiableList(neighbors);
		}
	}
	
	public static class PortalInfo {
		
		private final int cell1;
		private final int cell2;
		private final Point3D frame1;
		private final Point3D frame2;
		private final double height;
		
		public PortalInfo(int cell1, int cell2, Point3D frame1, Point3D frame2, double height) {
			this.cell1 = cell1;
			this.cell2 = cell2;
			this.frame1 = frame1;
			this.frame2 = frame2;
			this.height = height;
		}
		
		public int getOtherCell(int cell) {
			assert cell1 == cell || cell2 == cell;
			return cell1 == cell ? cell2 : cell1;
		}
		
		public int getCell1() {
			return cell1;
		}
		
		public int getCell2() {
			return cell2;
		}
		
		public Point3D getFrame1() {
			return frame1;
		}
		
		public Point3D getFrame2() {
			return frame2;
		}
		
		public double getHeight() {
			return height;
		}
		
	}
}
