/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.objects;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.CRC;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Log;

import intents.object.ObjectCreatedIntent;
import resources.buildout.BuildoutArea;
import resources.buildout.BuildoutArea.BuildoutAreaBuilder;
import resources.config.ConfigFile;
import resources.objects.SWGObject;
import resources.objects.SWGObject.ObjectClassification;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.server_info.DataManager;
import resources.server_info.SdbLoader;
import resources.server_info.SdbLoader.SdbResultSet;
import resources.server_info.StandardLog;

public class ClientBuildoutService extends Service {
	
	private static final String GET_ADDITIONAL_OBJECTS_SQL = "SELECT terrain, template, x, y, z, heading, cell_id, radius, building_name "
			+ "FROM additional_buildouts WHERE active = 1";
	private static final String GET_BUILDING_INFO_SQL = "SELECT object_id FROM buildings WHERE building_id = ?";
	
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
		objects.forEach(ObjectCreatedIntent::broadcast);
		objects.clear();
		return super.start();
	}
	
	public List<SWGObject> getClientObjects() {
		loadClientObjects();
		return Collections.unmodifiableList(objects);
	}
	
	private void loadClientObjects() {
		if (!objects.isEmpty())
			return;
		Map<Long, SWGObject> objects;
		long startTime = StandardLog.onStartLoad("client objects");
		try {
			loadAreas();
			if (DataManager.getConfig(ConfigFile.PRIMARY).getBoolean("LOAD-OBJECTS", true))
				objects = loadObjects();
			else
				objects = new HashMap<>();
		} catch (SQLException e) {
			objects = new HashMap<>();
			Log.e(e);
		}
		StandardLog.onEndLoad(objects.size(), "client objects", startTime);
		this.objects.clear();
		this.objects.addAll(objects.values());
	}
	
	public Map<Long, SWGObject> loadClientObjectsByArea(int areaId) {
		try {
			AreaLoader areaLoader = new AreaLoader(new File("serverdata/buildout/areas.sdb"));
			areasById.put(areaId, areaLoader.getAreaById(areaId));
			return loadObjects();
		} catch (SQLException e) {
			Log.e(e);
			return new HashMap<>();
		}
	}
	
	private Map<Long, SWGObject> loadObjects() throws SQLException {
		BuildoutLoader loader = new BuildoutLoader(areasById, new File("serverdata/buildout/objects.sdb"));
		Map<Long, SWGObject> objects = loader.getAllObjects();
		addAdditionalObjects(objects);
		return objects;
	}
	
	private void addAdditionalObjects(Map<Long, SWGObject> buildouts) throws SQLException {
		try (RelationalServerData data = RelationalServerFactory.getServerData("buildout/additional_buildouts.db", "additional_buildouts")) {
			try (ResultSet set = data.executeQuery(GET_ADDITIONAL_OBJECTS_SQL)) {
				set.setFetchSize(4*1024);
				while (set.next()) {
					createAdditionalObject(buildouts, set);
				}
			}
		}
	}
	
	private void createAdditionalObject(Map<Long, SWGObject> buildouts, ResultSet set) throws SQLException {
		try {
			SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getString("template"));
			obj.setPosition(set.getFloat("x"), set.getFloat("y"), set.getFloat("z"));
			obj.setTerrain(Terrain.getTerrainFromName(set.getString("terrain")));
			obj.setHeading(set.getFloat("heading"));
			obj.setClassification(ObjectClassification.BUILDOUT);
			obj.setPrefLoadRange(set.getFloat("radius"));
			checkParent(buildouts, obj, set.getString("building_name"), set.getInt("cell_id"));
			buildouts.put(obj.getObjectId(), obj);
		} catch (NullPointerException e) {
			Log.e("File: %s", set.getString("template"));
		}
	}
	
	private void checkParent(Map<Long, SWGObject> objects, SWGObject obj, String buildingName, int cellId) throws SQLException {
		try (RelationalServerData data = RelationalServerFactory.getServerData("building/building.db", "buildings")) {
			try (PreparedStatement statement = data.prepareStatement(GET_BUILDING_INFO_SQL)) {
				statement.setString(1, buildingName);
				try (ResultSet set = statement.executeQuery()) {
					if (!set.next()) {
						Log.e("Unknown building name: %s", buildingName);
						return;
					}
					long buildingId = set.getLong("object_id");
					if (buildingId == 0)
						return;
					SWGObject buildingUncasted = objects.get(buildingId);
					if (buildingUncasted == null) {
						Log.e("Building not found in map: %s / %d", buildingName, buildingId);
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
			}
		}
	}
	
	private void loadAreas() throws SQLException {
		List <String> events = new ArrayList<>();
		for (String event : DataManager.getConfig(ConfigFile.FEATURES).getString("EVENTS", "").split(",")) {
			if (event.isEmpty())
				continue;
			events.add(event.toLowerCase(Locale.US));
		}
		
		AreaLoader areaLoader = new AreaLoader(new File("serverdata/buildout/areas.sdb"));
		for (BuildoutArea area : areaLoader.getAllAreas(events)) {
			areasById.put(area.getId(), area);
		}
	}
	
	private static class AreaLoader {
		
		public AreaLoader(File file) {
			
		}
		
		public List<BuildoutArea> getAllAreas(List<String> events) {
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
		
		public BuildoutArea getAreaById(int areaId) {
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
		
		private BuildoutArea parseLine(SdbResultSet set) {
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
		
		private final Map<Integer, BuildoutArea> areas;
		private final ObjectCreationData creationData;
		private BuildoutArea currentArea;
		
		public BuildoutLoader(Map<Integer, BuildoutArea> areas, File file) {
			this.areas = areas;
			this.creationData = new ObjectCreationData();
			this.currentArea = areas.values().iterator().next();
		}
		
		public Map<Long, SWGObject> getAllObjects() {
			Map<Long, SWGObject> objects = new HashMap<>();
			int areaId;
			SWGObject object;
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
					parseLine(set);
					object = createObject(objects);
					objects.put(object.getObjectId(), object);
				}
			} catch (IOException e) {
				Log.e(e);
			}
			return objects;
		}
		
		private SWGObject createObject(Map<Long, SWGObject> objects) {
			SWGObject obj = ObjectCreator.createObjectFromTemplate(creationData.id, CRC.getString(creationData.templateCrc));
			obj.setClassification(creationData.snapshot ? ObjectClassification.SNAPSHOT : ObjectClassification.BUILDOUT);
			obj.setPrefLoadRange(creationData.radius);
			setObjectLocation(obj);
			setCellNumber(objects, obj);
			if (obj instanceof BuildingObject)
				((BuildingObject) obj).populateCells();
			return obj;
		}
		
		private void setObjectLocation(SWGObject obj) {
			obj.setLocation(Location.builder()
					.setPosition(creationData.x, creationData.y, creationData.z)
					.setOrientation(creationData.orientationX, creationData.orientationY, creationData.orientationZ, creationData.orientationW)
					.setTerrain(currentArea.getTerrain())
					.build());
		}
		
		private void setCellNumber(Map<Long, SWGObject> objects, SWGObject obj) {
			if (creationData.cellIndex != 0) {
				BuildingObject building = (BuildingObject) objects.get(creationData.containerId);
				CellObject cell = building.getCellByNumber(creationData.cellIndex);
				obj.moveToContainer(cell);
			}
		}
		
		private void parseLine(SdbResultSet set) {
			creationData.id				= set.getInt(0);
			creationData.snapshot		= set.getInt(1) != 0;
			creationData.templateCrc	= (int) set.getInt(3);
			creationData.containerId	= set.getInt(4);
			creationData.x				= set.getReal(5);
			creationData.y				= set.getReal(6);
			creationData.z				= set.getReal(7);
			creationData.orientationX	= set.getReal(8);
			creationData.orientationY	= set.getReal(9);
			creationData.orientationZ	= set.getReal(10);
			creationData.orientationW	= set.getReal(11);
			creationData.radius			= set.getReal(12);
			creationData.cellIndex		= (int) set.getInt(13);
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
