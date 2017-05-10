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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.CRC;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Log;

import resources.buildout.BuildoutArea;
import resources.buildout.BuildoutArea.BuildoutAreaBuilder;
import resources.config.ConfigFile;
import resources.objects.SWGObject;
import resources.objects.SWGObject.ObjectClassification;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.server_info.DataManager;
import resources.server_info.StandardLog;

public class ClientBuildoutService extends Service {
	
	private static final String GET_ADDITIONAL_OBJECTS_SQL = "SELECT terrain, template, x, y, z, heading, cell_id, radius, building_name "
			+ "FROM additional_buildouts WHERE active = 1";
	private static final String GET_BUILDING_INFO_SQL = "SELECT object_id FROM buildings WHERE building_id = ?";
	
	private final Map<Integer, BuildoutArea> areasById;
	
	public ClientBuildoutService() {
		areasById = new HashMap<>(1000); // Number of buildout areas
	}
	
	public Map<Long, SWGObject> loadClientObjects() {
		Map<Long, SWGObject> objects;
		long startTime = StandardLog.onStartLoad("client objects");
		try {
			loadAreas(getEvents());
			if (DataManager.getConfig(ConfigFile.PRIMARY).getBoolean("LOAD-OBJECTS", true))
				objects = loadObjects();
			else
				objects = new HashMap<>();
		} catch (SQLException e) {
			objects = new HashMap<>();
			Log.e(e);
		}
		StandardLog.onEndLoad(objects.size(), "client objects", startTime);
		return objects;
	}
	
	public Map<Long, SWGObject> loadClientObjectsByArea(int areaId) {
		try {
			try (AreaLoader areaLoader = new AreaLoader(new File("serverdata/buildout/areas.sdb"))) {
				areasById.put(areaId, areaLoader.getAreaById(areaId));
			}
			return loadObjects();
		} catch (SQLException e) {
			Log.e(e);
			return new HashMap<>();
		}
	}
	
	private Map<Long, SWGObject> loadObjects() throws SQLException {
		Map<Long, SWGObject> objects;
		try (BuildoutLoader loader = new BuildoutLoader(areasById, new File("serverdata/buildout/objects.sdb"))) {
			objects = loader.getAllObjects();
		}
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
	
	private List<String> getEvents() {
		List <String> events = new ArrayList<>();
		String eventStr = DataManager.getConfig(ConfigFile.FEATURES).getString("EVENTS", "");
		String [] eventArray = eventStr.split(",");
		for (String event : eventArray) {
			event = event.toLowerCase(Locale.US);
			if (!event.isEmpty())
				events.add(event);
		}
		return events;
	}
	
	private void loadAreas(List <String> events) throws SQLException {
		try (AreaLoader areaLoader = new AreaLoader(new File("serverdata/buildout/areas.sdb"))) {
			for (BuildoutArea area : areaLoader.getAllAreas(events)) {
				areasById.put(area.getId(), area);
			}
		}
	}
	
	private static class AreaLoader implements AutoCloseable {
		
		private final SdbLoader loader;
		
		public AreaLoader(File file) {
			this.loader = new SdbLoader(file);
			loader.loadNextLine(); // Skip column names
			loader.loadNextLine(); // Skip data types
		}
		
		@Override
		public void close() {
			loader.close();
		}
		
		public List<BuildoutArea> getAllAreas(List<String> events) {
			String line;
			Map<String, BuildoutArea> areas = new HashMap<>();
			BuildoutArea area;
			while ((line = loader.loadNextLine()) != null) {
				area = parseLine(line);
				BuildoutArea replaced = areas.get(area.getName());
				if (replaced == null || (!area.getEvent().isEmpty() && events.contains(area.getEvent()))) {
					areas.put(area.getName(), area);
				}
			}
			return new ArrayList<>(areas.values());
		}
		
		public BuildoutArea getAreaById(int areaId) {
			String line;
			String areaIdStr = Integer.toString(areaId);
			while ((line = loader.loadNextLine()) != null) {
				if (line.startsWith(areaIdStr)) {
					return parseLine(line);
				}
			}
			return null;
		}
		
		private BuildoutArea parseLine(String line) {
			int prevIndex = 0;
			int nextIndex = 0;
			BuildoutAreaBuilder builder = new BuildoutAreaBuilder();
			for (int i = 0; i < 10; i++) { // 10 expected variables
				nextIndex = line.indexOf('\t', prevIndex);
				parseValue(builder, line.substring(prevIndex, nextIndex), i);
				prevIndex = nextIndex+1;
			}
			parseValue(builder, line.substring(prevIndex), 10);
			return builder.build();
		}
		
		private void parseValue(BuildoutAreaBuilder builder, String str, int index) {
			// id	terrain	area_name	event	min_x	min_z	max_x	max_z	adjust_coordinates	translate_x	translate_z
			switch (index) {
				case 0:  builder.setId(Integer.parseInt(str)); break;
				case 1:  builder.setTerrain(Terrain.getTerrainFromName(str)); break;
				case 2:  builder.setName(str); break;
				case 3:  builder.setEvent(str); break;
				case 4:  builder.setX1(Double.parseDouble(str)); break;
				case 5:  builder.setZ1(Double.parseDouble(str)); break;
				case 6:  builder.setX2(Double.parseDouble(str)); break;
				case 7:  builder.setZ2(Double.parseDouble(str)); break;
				case 8:  builder.setAdjustCoordinates(Integer.parseInt(str) != 0); break;
				case 9:  builder.setTranslationX(Double.parseDouble(str)); break;
				case 10: builder.setTranslationZ(Double.parseDouble(str)); break;
			}
		}
		
	}
	
	private static class BuildoutLoader implements AutoCloseable {
		
		private final Map<Integer, BuildoutArea> areas;
		private final SdbLoader loader;
		private final ObjectCreationData creationData;
		private BuildoutArea currentArea;
		
		public BuildoutLoader(Map<Integer, BuildoutArea> areas, File file) {
			this.areas = areas;
			this.loader = new SdbLoader(file);
			this.creationData = new ObjectCreationData();
			this.currentArea = areas.values().iterator().next();
			loader.loadNextLine(); // Skip column names
			loader.loadNextLine(); // Skip data types
		}
		
		@Override
		public void close() {
			loader.close();
		}
		
		public Map<Long, SWGObject> getAllObjects() {
			Map<Long, SWGObject> objects = new HashMap<>();
			String line;
			int areaId;
			SWGObject object;
			while ((line = loader.loadNextLine()) != null) {
				areaId = getAreaId(line);
				if (currentArea.getId() != areaId) {
					BuildoutArea area = areas.get(areaId);
					if (area == null) {
						continue;
					}
					currentArea = area;
				}
				parseLine(line);
				object = createObject(objects);
				objects.put(object.getObjectId(), object);
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
			obj.setPosition(creationData.x, creationData.y, creationData.z);
			obj.setOrientation(creationData.orientationX, creationData.orientationY, creationData.orientationZ, creationData.orientationW);
			obj.setTerrain(currentArea.getTerrain());
		}
		
		private void setCellNumber(Map<Long, SWGObject> objects, SWGObject obj) {
			if (creationData.cellIndex != 0) {
				BuildingObject building = (BuildingObject) objects.get(creationData.containerId);
				CellObject cell = building.getCellByNumber(creationData.cellIndex);
				obj.moveToContainer(cell);
			}
		}
		
		private void parseLine(String line) {
			int prevIndex = 0;
			int nextIndex = 0;
			for (int i = 0; i < 13; i++) { // 14 expected variables
				nextIndex = line.indexOf('\t', prevIndex);
				parseValue(line.substring(prevIndex, nextIndex), i);
				prevIndex = nextIndex+1;
			}
			parseValue(line.substring(prevIndex), 13);
		}
		
		private int getAreaId(String line) {
			int nextIndex = 0;
			nextIndex = line.indexOf('\t', nextIndex); // move to column 2
			nextIndex = line.indexOf('\t', nextIndex+1); // move to column 3
			int prevIndex = nextIndex;
			nextIndex = line.indexOf('\t', nextIndex+1); // move to column 3
			return Integer.parseInt(line.substring(prevIndex+1, nextIndex));
		}
		
		private void parseValue(String str, int index) {
			switch (index) {
				case 0: creationData.id				= Long.parseLong(str); break;
				case 1: creationData.snapshot		= Long.parseLong(str) != 0; break;
				case 2: creationData.areaId			= Integer.parseInt(str); break;
				case 3: creationData.templateCrc	= Integer.parseInt(str); break;
				case 4: creationData.containerId	= Long.parseLong(str); break;
				case 5: creationData.x				= Double.parseDouble(str); break;
				case 6: creationData.y				= Double.parseDouble(str); break;
				case 7: creationData.z				= Double.parseDouble(str); break;
				case 8: creationData.orientationX	= Double.parseDouble(str); break;
				case 9: creationData.orientationY	= Double.parseDouble(str); break;
				case 10: creationData.orientationZ	= Double.parseDouble(str); break;
				case 11: creationData.orientationW	= Double.parseDouble(str); break;
				case 12: creationData.radius		= Double.parseDouble(str); break;
				case 13: creationData.cellIndex		= Integer.parseInt(str); break;
			}
		}
		
	}
	
	private static class ObjectCreationData {
		
		public long id;
		public boolean snapshot;
		public int areaId;
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
	
	private static class SdbLoader implements AutoCloseable {
		
		private final BufferedReader reader;
		
		public SdbLoader(File file) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(file));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				br = null;
			}
			this.reader = br;
		}
		
		@Override
		public void close() {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public String loadNextLine() {
			try {
				return reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		
	}
	
}
