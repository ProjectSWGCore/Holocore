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
import com.projectswg.common.data.CRC;
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
		loadClientObjects();
		return Collections.unmodifiableList(objects);
	}
	
	private static void broadcast(SWGObject obj) {
		ObjectCreatedIntent.broadcast(obj);
		obj.getContainedObjects().forEach(ClientBuildoutService::broadcast);
		obj.getSlottedObjects().forEach(ClientBuildoutService::broadcast);
	}
	
	private void loadClientObjects() {
		if (!objects.isEmpty())
			return;
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
			obj.setPrefLoadRange(set.getReal("radius"));
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
		obj.moveToContainer(cell);
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
		
		private BuildoutLoader() {
			
		}
		
		public static Map<Long, SWGObject> getAllObjects(Map<Integer, BuildoutArea> areas) {
			Map<Long, SWGObject> objects = new HashMap<>();
			int areaId;
			SWGObject object;
			ObjectCreationData data = new ObjectCreationData();
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
					parseLine(set, data);
					object = createObject(objects, data, currentArea);
					objects.put(object.getObjectId(), object);
				}
			} catch (IOException e) {
				Log.e(e);
			}
			return objects;
		}
		
		private static SWGObject createObject(Map<Long, SWGObject> objects, ObjectCreationData data, BuildoutArea area) {
			SWGObject obj = ObjectCreator.createObjectFromTemplate(data.id, CRC.getString(data.templateCrc));
			obj.setClassification(data.snapshot ? ObjectClassification.SNAPSHOT : ObjectClassification.BUILDOUT);
			obj.setPrefLoadRange(data.radius);
			setObjectLocation(obj, data, area);
			setCellNumber(objects, obj, data);
			if (obj instanceof BuildingObject)
				((BuildingObject) obj).populateCells();
			return obj;
		}
		
		private static void setObjectLocation(SWGObject obj, ObjectCreationData data, BuildoutArea area) {
			obj.setLocation(Location.builder()
					.setPosition(data.x, data.y, data.z)
					.setOrientation(data.orientationX, data.orientationY, data.orientationZ, data.orientationW)
					.setTerrain(area.getTerrain())
					.build());
		}
		
		private static void setCellNumber(Map<Long, SWGObject> objects, SWGObject obj, ObjectCreationData data) {
			if (data.cellIndex != 0) {
				BuildingObject building = (BuildingObject) objects.get(data.containerId);
				CellObject cell = building.getCellByNumber(data.cellIndex);
				obj.moveToContainer(cell);
			}
		}
		
		private static void parseLine(SdbResultSet set, ObjectCreationData data) {
			data.id				= set.getInt(0);
			data.snapshot		= set.getInt(1) != 0;
			data.templateCrc	= (int) set.getInt(3);
			data.containerId	= set.getInt(4);
			data.x				= set.getReal(5);
			data.y				= set.getReal(6);
			data.z				= set.getReal(7);
			data.orientationX	= set.getReal(8);
			data.orientationY	= set.getReal(9);
			data.orientationZ	= set.getReal(10);
			data.orientationW	= set.getReal(11);
			data.radius			= set.getReal(12);
			data.cellIndex		= (int) set.getInt(13);
		}
		
	}
	
	private static class ObjectCreationData {
		
		public long id;
		public boolean snapshot;
		public int templateCrc;
		public long containerId;
		public double x;
		public double y;
		public double z;
		public double orientationX;
		public double orientationY;
		public double orientationZ;
		public double orientationW;
		public double radius;
		public int cellIndex;
		
	}
	
}
