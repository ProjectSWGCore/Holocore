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

import com.projectswg.common.data.CrcDatabase;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.ObjectCreator.ObjectCreationException;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BuildoutLoader {
	
	private static final CrcDatabase CRC_DATABASE = CrcDatabase.getInstance();
	
	private final Map<Long, SWGObject> objectMap;
	private final Map<String, BuildingObject> buildingMap;
	private final EnumMap<Terrain, Map<Long, SWGObject>> terrainMap;
	private final Set<String> events;
	
	private BuildoutLoader(Collection<String> events) {
		this.objectMap = new ConcurrentHashMap<>();
		this.buildingMap = new ConcurrentHashMap<>();
		this.terrainMap = new EnumMap<>(Terrain.class);
		this.events = new HashSet<>(events);
		
		for (Terrain terrain : Terrain.values()) {
			terrainMap.put(terrain, new HashMap<>());
		}
	}
	
	public Map<Long, SWGObject> getObjects() {
		return Collections.unmodifiableMap(objectMap);
	}
	
	public Map<String, BuildingObject> getBuildings() {
		return Collections.unmodifiableMap(buildingMap);
	}
	
	public Map<Long, SWGObject> getObjects(Terrain terrain) {
		return terrainMap.get(terrain);
	}
	
	private void loadFromFile() {
		loadStandardBuildouts();
		loadAdditionalBuildouts();
	}
	
	private void loadStandardBuildouts() {
		Set<String> events = this.events;
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/objects.sdb"))) {
			while (set.next()) {
				// "id", "template_crc", "container_id", "event", "terrain", "x", "y", "z", "orientation_x", "orientation_y", "orientation_z", "orientation_w", "cell_index", "tag"
				String event = set.getText(3);
				if (!event.isEmpty() && !events.contains(event))
					continue;

				SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getInt(0), CRC_DATABASE.getString((int) set.getInt(1)));
				obj.setGenerated(false);
				obj.setLocation(Location.builder().setPosition(set.getReal(5), set.getReal(6), set.getReal(7)).setOrientation(set.getReal(8), set.getReal(9), set.getReal(10), set.getReal(11))
						.setTerrain(Terrain.valueOf(set.getText(4))).build());
				obj.setBuildoutTag(set.getText(13));
				if (set.getInt(12) != 0) {
					BuildingObject building = (BuildingObject) objectMap.get(set.getInt(2));
					CellObject cell = building.getCellByNumber((int) set.getInt(12));
					obj.systemMove(cell);
				} else if (obj instanceof BuildingObject) {
					((BuildingObject) obj).populateCells();
					for (SWGObject cell : obj.getContainedObjects())
						objectMap.put(cell.getObjectId(), cell);
					if (!obj.getBuildoutTag().isEmpty())
						buildingMap.put(obj.getBuildoutTag(), (BuildingObject) obj);
				}
				objectMap.put(set.getInt(0), obj);
			}
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	private void loadAdditionalBuildouts() {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/additional_buildouts.sdb"))) {
			while (set.next()) {
				if (!set.getBoolean("active"))
					continue;
				createAdditionalObject(set);
			}
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	private void createAdditionalObject(SdbResultSet set) {
		try {
			SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getText("template"));
			obj.setPosition(set.getReal("x"), set.getReal("y"), set.getReal("z"));
			obj.setTerrain(Terrain.getTerrainFromName(set.getText("terrain")));
			obj.setHeading(set.getReal("heading"));
			obj.setGenerated(false);
			checkParent(obj, set.getText("building_name"), (int) set.getInt("cell_id"));
			if (obj instanceof BuildingObject) {
				((BuildingObject) obj).populateCells();
				for (SWGObject cell : obj.getContainedObjects())
					objectMap.put(cell.getObjectId(), cell);
			}
			objectMap.put(obj.getObjectId(), obj);
		} catch (ObjectCreationException e) {
			Log.e("Invalid additional object: %s", set.getText("template"));
		}
	}
	
	private void checkParent(SWGObject obj, String buildingName, int cellId) {
		if (buildingName.endsWith("_world"))
			return;
		BuildingObject building = buildingMap.get(buildingName);
		if (building == null) {
			Log.e("Building not found in map: %s", buildingName);
			return;
		}
		
		CellObject cell = building.getCellByNumber(cellId);
		if (cell == null) {
			Log.e("Cell is not found! Building: %s Cell: %d", buildingName, cellId);
			return;
		}
		obj.systemMove(cell);
	}
	
	static BuildoutLoader load(Collection<String> events) {
		BuildoutLoader loader = new BuildoutLoader(events);
		loader.loadFromFile();
		return loader;
	}
	
	private static class BuildoutInfo {
		
		private final long id;
		private final int crc;
		private final long containerId;
		private final String event;
		private final Terrain terrain;
		private final double x;
		private final double y;
		private final double z;
		private final double orientationX;
		private final double orientationY;
		private final double orientationZ;
		private final double orientationW;
		private final int cellIndex;
		private final String tag;
		
		public BuildoutInfo(SdbResultSet set) {
			this.id = set.getInt("id");
			this.crc = (int) set.getInt("template_crc");
			this.containerId = set.getInt("container_id");
			this.event = set.getText("event");
			this.terrain = Terrain.valueOf(set.getText("terrain"));
			this.x = set.getReal("x");
			this.y = set.getReal("y");
			this.z = set.getReal("z");
			this.orientationX = set.getReal("orientation_x");
			this.orientationY = set.getReal("orientation_y");
			this.orientationZ = set.getReal("orientation_z");
			this.orientationW = set.getReal("orientation_w");
			this.cellIndex = (int) set.getInt("cell_index");
			this.tag = set.getText("tag");
		}
		
		public long getId() {
			return id;
		}
		
		public int getCrc() {
			return crc;
		}
		
		public long getContainerId() {
			return containerId;
		}
		
		public String getEvent() {
			return event;
		}
		
		public Terrain getTerrain() {
			return terrain;
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
		
		public double getOrientationX() {
			return orientationX;
		}
		
		public double getOrientationY() {
			return orientationY;
		}
		
		public double getOrientationZ() {
			return orientationZ;
		}
		
		public double getOrientationW() {
			return orientationW;
		}
		
		public int getCellIndex() {
			return cellIndex;
		}
		
		public String getTag() {
			return tag;
		}
	}
	
}
