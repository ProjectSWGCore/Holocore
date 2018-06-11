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
import com.projectswg.holocore.resources.support.objects.buildout.BuildoutArea;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuildingLoader.BuildingLoaderInfo;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.ObjectCreator.ObjectCreationException;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class BuildoutLoader {
	
	private static final CrcDatabase CRC_DATABASE = CrcDatabase.getInstance();
	
	private final Map<Long, SWGObject> objectMap;
	private final EnumMap<Terrain, Map<Long, SWGObject>> terrainMap;
	private final AreaLoader areaLoader;
	
	private BuildoutLoader(AreaLoader areaLoader) {
		this.objectMap = new HashMap<>();
		this.terrainMap = new EnumMap<>(Terrain.class);
		this.areaLoader = areaLoader;
		
		for (Terrain terrain : Terrain.values()) {
			terrainMap.put(terrain, new HashMap<>());
		}
	}
	
	public Map<Long, SWGObject> getObjects() {
		return objectMap;
	}
	
	public Map<Long, SWGObject> getObjects(Terrain terrain) {
		return terrainMap.get(terrain);
	}
	
	private void loadFromFile() {
		loadStandardBuildouts();
		loadAdditionalBuildouts();
	}
	
	private void loadStandardBuildouts() {
		int areaId;
		AreaLoader areaLoader = this.areaLoader;
		BuildoutArea currentArea = areaLoader.getAreaList().get(0);
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/objects.sdb"))) {
			while (set.next()) {
				areaId = (int) set.getInt(1);
				if (currentArea.getId() != areaId) {
					BuildoutArea area = areaLoader.getArea(areaId);
					if (area == null) { // usually for events
						continue;
					}
					currentArea = area;
				}
				
				SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getInt(0), CRC_DATABASE.getString((int) set.getInt(2)));
				obj.setGenerated(false);
				obj.setLocation(Location.builder().setPosition(set.getReal(4), set.getReal(5), set.getReal(6)).setOrientation(set.getReal(7), set.getReal(8), set.getReal(9), set.getReal(10))
						.setTerrain(currentArea.getTerrain()).build());
				if (set.getInt(12) != 0) {
					BuildingObject building = (BuildingObject) objectMap.get(set.getInt(3));
					CellObject cell = building.getCellByNumber((int) set.getInt(12));
					obj.systemMove(cell);
				} else if (obj instanceof BuildingObject) {
					((BuildingObject) obj).populateCells();
					for (SWGObject cell : obj.getContainedObjects())
						objectMap.put(cell.getObjectId(), cell);
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
		BuildingLoaderInfo building = DataLoader.buildings().getBuilding(buildingName);
		if (building == null) {
			Log.e("Building not found in map: %s", buildingName);
			return;
		}
		long buildingId = building.getId();
		if (buildingId == 0)
			return; // World
		
		SWGObject buildingUncasted = objectMap.get(buildingId);
		if (buildingUncasted == null) {
			Log.e("Building not found in map: %s", buildingName);
			return;
		}
		if (!(buildingUncasted instanceof BuildingObject)) {
			Log.e("Building is not an instance of BuildingObject: %s", buildingName);
			return;
		}
		CellObject cell = ((BuildingObject) buildingUncasted).getCellByNumber(cellId);
		if (cell == null) {
			Log.e("Cell is not found! Building: %s Cell: %d", buildingName, cellId);
			return;
		}
		obj.systemMove(cell);
	}
	
	static BuildoutLoader load(AreaLoader areaLoader) {
		BuildoutLoader loader = new BuildoutLoader(areaLoader);
		loader.loadFromFile();
		return loader;
	}
	
}
