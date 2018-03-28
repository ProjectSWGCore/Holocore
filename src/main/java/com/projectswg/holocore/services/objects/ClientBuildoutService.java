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
package com.projectswg.holocore.services.objects;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.CrcDatabase;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Log;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.buildout.BuildoutArea;
import com.projectswg.holocore.resources.buildout.BuildoutArea.BuildoutAreaBuilder;
import com.projectswg.holocore.resources.config.ConfigFile;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.SWGObject.ObjectClassification;
import com.projectswg.holocore.resources.objects.building.BuildingObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.resources.server_info.DataManager;
import com.projectswg.holocore.resources.server_info.SdbLoader;
import com.projectswg.holocore.resources.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.server_info.StandardLog;
import com.projectswg.holocore.resources.server_info.loader.BuildingLoader;
import com.projectswg.holocore.resources.server_info.loader.BuildingLoader.BuildingLoaderInfo;
import com.projectswg.holocore.services.objects.ObjectCreator.ObjectCreationException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClientBuildoutService extends Service {
	
	private final Map<Integer, BuildoutArea> areasById;
	private final List<SWGObject> objects;
	
	public ClientBuildoutService() {
		this.areasById = new HashMap<>(1000); // Number of buildout areas
		this.objects = new ArrayList<>();
	}
	
	@Override
	public boolean initialize() {
		loadClientObjects();
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		objects.forEach(ClientBuildoutService::broadcast);
		objects.clear();
		return super.start();
	}
	
	public List<SWGObject> getClientObjects() {
		if (!objects.isEmpty())
			return Collections.unmodifiableList(this.objects);
		Map<Long, SWGObject> objects;
		long startTime = StandardLog.onStartLoad("client objects");
		loadAreas();
		if (DataManager.getConfig(ConfigFile.PRIMARY).getBoolean("LOAD-OBJECTS", true))
			objects = loadObjects();
		else
			objects = new HashMap<>();
		StandardLog.onEndLoad(objects.size(), "client objects", startTime);
		this.objects.clear();
		this.objects.addAll(objects.values());
		return Collections.unmodifiableList(this.objects);
	}
	
	public List<SWGObject> getClientObjects(Terrain terrain) {
		if (!objects.isEmpty())
			return Collections.unmodifiableList(this.objects);
		Map<Long, SWGObject> objects;
		long startTime = StandardLog.onStartLoad("client objects");
		loadAreas();
		if (DataManager.getConfig(ConfigFile.PRIMARY).getBoolean("LOAD-OBJECTS", true))
			objects = loadObjects(terrain);
		else
			objects = new HashMap<>();
		StandardLog.onEndLoad(objects.size(), "client objects", startTime);
		this.objects.clear();
		this.objects.addAll(objects.values());
		return Collections.unmodifiableList(this.objects);
	}
	
	private static void broadcast(SWGObject obj) {
		ObjectCreatedIntent.broadcast(obj);
		obj.getContainedObjects().forEach(ClientBuildoutService::broadcast);
		obj.getSlottedObjects().forEach(ClientBuildoutService::broadcast);
	}
	
	private void loadClientObjects() {
	}
	
	public Map<Long, SWGObject> loadClientObjectsByArea(int areaId) {
		areasById.put(areaId, AreaLoader.getAreaById(areaId));
		return loadObjects();
	}
	
	private Map<Long, SWGObject> loadObjects() {
		Map<Long, SWGObject> objects = BuildoutLoader.getAllObjects(areasById);
		addAdditionalObjects(objects);
		return objects;
	}
	
	private Map<Long, SWGObject> loadObjects(Terrain terrain) {
		Map<Long, SWGObject> objects = BuildoutLoader.getAllObjects(areasById, terrain);
		addAdditionalObjects(objects);
		return objects;
	}
	
	private void addAdditionalObjects(Map<Long, SWGObject> buildouts) {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/additional_buildouts.sdb"))) {
			while (set.next()) {
				if (!set.getBoolean("active"))
					continue;
				createAdditionalObject(buildouts, set);
			}
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	private void createAdditionalObject(Map<Long, SWGObject> buildouts, SdbResultSet set) {
		try {
			SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getText("template"));
			obj.setPosition(set.getReal("x"), set.getReal("y"), set.getReal("z"));
			obj.setTerrain(Terrain.getTerrainFromName(set.getText("terrain")));
			obj.setHeading(set.getReal("heading"));
			obj.setClassification(ObjectClassification.BUILDOUT);
			checkParent(buildouts, obj, set.getText("building_name"), (int) set.getInt("cell_id"));
			buildouts.put(obj.getObjectId(), obj);
		} catch (ObjectCreationException e) {
			Log.e("Invalid additional object: %s", set.getText("template"));
		}
	}
	
	private void checkParent(Map<Long, SWGObject> objects, SWGObject obj, String buildingName, int cellId) {
		BuildingLoaderInfo building = BuildingLoader.load().getBuilding(buildingName);
		if (building == null) {
			Log.e("Building not found in map: %s", buildingName);
			return;
		}
		long buildingId = building.getId();
		if (buildingId == 0)
			return; // World
		
		SWGObject buildingUncasted = objects.get(buildingId);
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
	
	private void loadAreas() {
		List <String> events = new ArrayList<>();
		for (String event : DataManager.getConfig(ConfigFile.FEATURES).getString("EVENTS", "").split(",")) {
			if (event.isEmpty())
				continue;
			events.add(event.toLowerCase(Locale.US));
		}
		
		for (BuildoutArea area : AreaLoader.getAllAreas(events)) {
			areasById.put(area.getId(), area);
		}
	}
	
	private static class AreaLoader {
		
		public static List<BuildoutArea> getAllAreas(List<String> events) {
			Map<String, BuildoutArea> areas = new HashMap<>();
			BuildoutArea area;
			try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/areas.sdb"))) {
				while (set.next()) {
					area = parseLine(set);
					BuildoutArea replaced = areas.get(area.getName());
					if ((replaced == null && area.getEvent().isEmpty()) || (!area.getEvent().isEmpty() && events.contains(area.getEvent()))) {
						areas.put(area.getName(), area);
					}
				}
			} catch (IOException e) {
				Log.e(e);
			}
			return new ArrayList<>(areas.values());
		}
		
		public static BuildoutArea getAreaById(int areaId) {
			try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/areas.sdb"))) {
				while (set.next()) {
					if (set.getInt(0) == areaId)
						return parseLine(set);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		private static BuildoutArea parseLine(SdbResultSet set) {
			return new BuildoutAreaBuilder()
					.setId((int) set.getInt(0))
					.setTerrain(Terrain.getTerrainFromName(set.getText(1)))
					.setName(set.getText(2))
					.setEvent(set.getText(3))
					.setX1(set.getReal(4))
					.setZ1(set.getReal(5))
					.setX2(set.getReal(6))
					.setZ2(set.getReal(7))
					.setAdjustCoordinates(set.getInt(8) != 0)
					.setTranslationX(set.getReal(9))
					.setTranslationX(set.getReal(10))
					.build();
		}
		
	}
	
	private static class BuildoutLoader {
		
		private static final CrcDatabase CRC_DATABASE = CrcDatabase.getInstance();
		
		private BuildoutLoader() {
			
		}
		
		public static Map<Long, SWGObject> getAllObjects(Map<Integer, BuildoutArea> areas) {
			Map<Long, SWGObject> objects = new HashMap<>();
			int areaId;
			BuildoutArea currentArea = areas.values().iterator().next();
			try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/objects.sdb"))) {
				while (set.next()) {
					areaId = (int) set.getInt(2);
					if (currentArea.getId() != areaId) {
						BuildoutArea area = areas.get(areaId);
						if (area == null) { // usually for events
							continue;
						}
						currentArea = area;
					}
					createObject(objects, set, currentArea);
				}
			} catch (IOException e) {
				Log.e(e);
			}
			return objects;
		}
		
		public static Map<Long, SWGObject> getAllObjects(Map<Integer, BuildoutArea> areas, Terrain terrain) {
			Map<Long, SWGObject> objects = new HashMap<>();
			int areaId;
			BuildoutArea currentArea = areas.values().iterator().next();
			try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/objects.sdb"))) {
				while (set.next()) {
					areaId = (int) set.getInt(2);
					if (currentArea.getId() != areaId) {
						BuildoutArea area = areas.get(areaId);
						if (area == null) { // usually for events
							continue;
						}
						currentArea = area;
					}
					if (currentArea.getTerrain() != terrain)
						continue;
					createObject(objects, set, currentArea);
				}
			} catch (IOException e) {
				Log.e(e);
			}
			return objects;
		}
		
		private static void createObject(Map<Long, SWGObject> objects, SdbResultSet set, BuildoutArea area) {
			SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getInt(0), CRC_DATABASE.getString((int) set.getInt(3)));
			obj.setClassification(set.getInt(1) != 0 ? ObjectClassification.SNAPSHOT : ObjectClassification.BUILDOUT);
			obj.setLocation(Location.builder()
					.setPosition(set.getReal(5), set.getReal(6), set.getReal(7))
					.setOrientation(set.getReal(8), set.getReal(9), set.getReal(10), set.getReal(11))
					.setTerrain(area.getTerrain())
					.build());
			if (set.getInt(13) != 0) {
				BuildingObject building = (BuildingObject) objects.get(set.getInt(4));
				CellObject cell = building.getCellByNumber((int) set.getInt(13));
				obj.systemMove(cell);
			}
			if (obj instanceof BuildingObject)
				((BuildingObject) obj).populateCells();
			objects.put(set.getInt(0), obj);
		}
		
	}
	
}
