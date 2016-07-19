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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import intents.object.ObjectCreatedIntent;
import intents.player.PlayerTransformedIntent;
import resources.Location;
import resources.Terrain;
import resources.buildout.BuildoutArea;
import resources.buildout.BuildoutArea.BuildoutAreaBuilder;
import resources.buildout.BuildoutAreaGrid;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.SWGObject.ObjectClassification;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.server_info.CrcDatabase;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class ClientBuildoutService extends Service {
	
	private static final String GET_BUILDOUT_AREAS = "SELECT * FROM areas ORDER BY area_name ASC, event ASC";
	private static final String GET_CLIENT_OBJECTS_SQL = "SELECT objects.id, objects.snapshot, objects.area_id, objects.template_crc, objects.container_id, "
			+ "objects.x, objects.y, objects.z, objects.orientation_x, objects.orientation_y, objects.orientation_z, objects.orientation_w, "
			+ "objects.radius, objects.cell_index "
			+ "FROM objects "
			+ "ORDER BY buildout_id ASC";
	private static final String GET_ADDITIONAL_OBJECTS_SQL = "SELECT terrain, template, x, y, z, heading, cell_id, radius, building_name "
			+ "FROM additional_buildouts WHERE active = 1";
	private static final String GET_BUILDING_INFO_SQL = "SELECT object_id FROM buildings WHERE building_id = ?";
	
	private final BuildoutAreaGrid areaGrid;
	private final Map<Integer, BuildoutArea> areasById;
	
	public ClientBuildoutService() {
		areaGrid = new BuildoutAreaGrid();
		areasById = new Hashtable<>(1000); // Number of buildout areas
		
		registerForIntent(PlayerTransformedIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case PlayerTransformedIntent.TYPE:
				if (i instanceof PlayerTransformedIntent)
					handlePlayerTransform((PlayerTransformedIntent) i);
				break;
			case ObjectCreatedIntent.TYPE:
				if (i instanceof ObjectCreatedIntent)
					handleObjectCreated((ObjectCreatedIntent) i);
				break;
			default:
				break;
		}
	}
	
	public Collection<SWGObject> loadClientObjects() {
		Collection<SWGObject> objects;
		long startLoad = System.nanoTime();
		Log.i(this, "Loading client objects...");
		try {
			loadAreas(getEvents());
			if (getConfig(ConfigFile.PRIMARY).getBoolean("LOAD-OBJECTS", true))
				objects = loadObjects();
			else
				objects = new HashSet<>();
		} catch (SQLException e) {
			objects = new HashSet<>();
			e.printStackTrace();
			Log.e(this, e);
		}
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		Log.i(this, "Finished loading %d client objects. Time: %fms", objects.size(), loadTime);
		return objects;
	}
	
	private Collection<SWGObject> loadObjects() throws SQLException {
		Map<Long, SWGObject> objects = new Hashtable<>();
		try (CrcDatabase strings = new CrcDatabase()) {
			try (RelationalServerData data = RelationalServerFactory.getServerData("buildout/buildouts.db", "objects")) {
				try (ResultSet set = data.executeQuery(GET_CLIENT_OBJECTS_SQL)) {
					set.setFetchSize(4*1024);
					BuildoutArea area = null;
					ObjectInformation info = new ObjectInformation(set);
					int prevAreaId = Integer.MAX_VALUE;
					int areaId = 0;
					while (set.next()) {
						areaId = info.getAreaIdNoLoad();
						if (prevAreaId != areaId) {
							area = areasById.get(areaId);
							prevAreaId = areaId;
						}
						if (area == null)
							continue;
						info.load(strings);
						createObject(objects, area, info);
					}
				}
			}
		}
		List<SWGObject> ret = new ArrayList<>(objects.values());
		ret.addAll(getAdditionalObjects());
		return ret;
	}
	
	private Collection<SWGObject> getAdditionalObjects() throws SQLException {
		Map<Long, SWGObject> objects = new Hashtable<>();
		try (CrcDatabase strings = new CrcDatabase()) {
			try (RelationalServerData data = RelationalServerFactory.getServerData("buildout/additional_buildouts.db", "additional_buildouts")) {
				try (ResultSet set = data.executeQuery(GET_ADDITIONAL_OBJECTS_SQL)) {
					set.setFetchSize(4*1024);
					while (set.next()) {
						createAdditionalObject(objects, set);
					}
				}
			}
		}
		return new ArrayList<>(objects.values());
	}
	
	private void createObject(Map<Long, SWGObject> objects, BuildoutArea area, ObjectInformation info) throws SQLException {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(info.getId(), info.getTemplate());
		Location l = info.getLocation();
		l.setTerrain(area.getTerrain());
		obj.setLocation(l);
		obj.setClassification(info.isSnapshot() ? ObjectClassification.SNAPSHOT : ObjectClassification.BUILDOUT);
		obj.setLoadRange(info.getRadius());
		checkCell(obj, info.getCell());
		checkChild(objects, obj, info.getContainer());
		objects.put(obj.getObjectId(), obj);
	}
	
	private void createAdditionalObject(Map<Long, SWGObject> objects, ResultSet set) throws SQLException {
		try {
			SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getString("template"));
			Location l = new Location();
			l.setX(set.getFloat("x"));
			l.setY(set.getFloat("y"));
			l.setZ(set.getFloat("z"));
			l.setTerrain(Terrain.getTerrainFromName(set.getString("terrain")));
			l.setHeading(set.getFloat("heading"));
			obj.setLocation(l);
			obj.setClassification(ObjectClassification.BUILDOUT);
			obj.setLoadRange(set.getFloat("radius"));
			checkParent(objects, obj, set.getString("building_name"), set.getInt("cell_id"));
			objects.put(obj.getObjectId(), obj);
		} catch (NullPointerException e) {
			Log.e(this, "File: %s", set.getString("template"));
		}
	}
	
	private void checkParent(Map<Long, SWGObject> objects, SWGObject obj, String buildingName, int cellId) throws SQLException {
		try (RelationalServerData data = RelationalServerFactory.getServerData("building/building.db", "buildings")) {
			try (PreparedStatement statement = data.prepareStatement(GET_BUILDING_INFO_SQL)) {
				statement.setString(1, buildingName);
				try (ResultSet set = statement.executeQuery()) {
					if (!set.next())
						return;
					SWGObject buildingUncasted = objects.get(set.getLong("object_id"));
					if (!(buildingUncasted instanceof BuildingObject))
						return;
					obj.moveToContainer(((BuildingObject) buildingUncasted).getCellByNumber(cellId));
				}
			}
		}
	}
	
	private void checkCell(SWGObject obj, int cell) {
		if (cell != 0 && obj instanceof CellObject)
			((CellObject) obj).setNumber(cell);
	}
	
	private void checkChild(Map<Long, SWGObject> objects, SWGObject obj, long container) {
		if (container != 0)
			obj.moveToContainer(objects.get(container));
	}
	
	private List<String> getEvents() {
		List <String> events = new ArrayList<>();
		String eventStr = getConfig(ConfigFile.FEATURES).getString("EVENTS", "");
		String [] eventArray = eventStr.split(",");
		for (String event : eventArray) {
			event = event.toLowerCase(Locale.US);
			if (!event.isEmpty())
				events.add(event);
		}
		return events;
	}
	
	private void loadAreas(List <String> events) throws SQLException {
		BuildoutArea primary = null; // Stored as "best area" for what we want to load
		try (RelationalServerData data = RelationalServerFactory.getServerData("buildout/areas.db", "areas")) {
			try (ResultSet set = data.executeQuery(GET_BUILDOUT_AREAS)) {
				areaGrid.clear();
				areasById.clear();
				AreaIndexes ind = new AreaIndexes(set);
				boolean loaded = false;
				while (set.next()) {
					BuildoutArea area = createArea(set, ind);
					area.setLoaded(false);
					if (area.getEvent().isEmpty() && (primary == null || !area.getName().equals(primary.getName()))) {
						if (!loaded && primary != null)
							loadArea(area);
						loaded = false;
						primary = area; // Primary area, no event
					}
					if (events.contains(area.getEvent())) {
						loadArea(area);
						loaded = true;
					}
				}
				if (!loaded && primary != null)
					loadArea(primary);
			}
		}
	}
	
	private void loadArea(BuildoutArea area) {
		area.setLoaded(true);
		areaGrid.addBuildoutArea(area);
		areasById.put(area.getId(), area);
	}
	
	private void handlePlayerTransform(PlayerTransformedIntent pti) {
		setObjectArea(pti.getPlayer());
	}
	
	private void handleObjectCreated(ObjectCreatedIntent oci) {
		setObjectArea(oci.getObject());
	}
	
	private void setObjectArea(SWGObject obj) {
		if (obj.getParent() != null) {
			obj.setBuildoutArea(null);
			return;
		}
		Location world = obj.getWorldLocation();
		BuildoutArea area = obj.getBuildoutArea();
		if (area == null || !isWithin(area, world.getTerrain(), world.getX(), world.getZ())) {
			area = getAreaForObject(obj);
			obj.setBuildoutArea(area);
		}
	}
	
	private BuildoutArea createArea(ResultSet set, AreaIndexes ind) throws SQLException {
		BuildoutAreaBuilder bldr = new BuildoutAreaBuilder()
			.setId(set.getInt(ind.id))
			.setName(set.getString(ind.name))
			.setTerrain(Terrain.getTerrainFromName(set.getString(ind.terrain)))
			.setEvent(set.getString(ind.event))
			.setX1(set.getDouble(ind.x1))
			.setZ1(set.getDouble(ind.z1))
			.setX2(set.getDouble(ind.x2))
			.setZ2(set.getDouble(ind.z2))
			.setAdjustCoordinates(set.getBoolean(ind.adjust))
			.setTranslationX(set.getDouble(ind.transX))
			.setTranslationZ(set.getDouble(ind.transZ));
		return bldr.build();
	}
	
	private BuildoutArea getAreaForObject(SWGObject obj) {
		Location l = obj.getWorldLocation();
		return areaGrid.getBuildoutArea(l.getTerrain(), l.getX(), l.getZ());
	}
	
	private boolean isWithin(BuildoutArea area, Terrain t, double x, double z) {
		return area.getTerrain() == t && x >= area.getX1() && x <= area.getX2() && z >= area.getZ1() && z <= area.getZ2();
	}
	
	private static class AreaIndexes {
		
		private int id;
		private int name;
		private int terrain;
		private int event;
		private int x1, z1, x2, z2;
		private int adjust;
		private int transX, transZ;
		
		public AreaIndexes(ResultSet set) throws SQLException {
			id = set.findColumn("id");
			name = set.findColumn("area_name");
			terrain = set.findColumn("terrain");
			event = set.findColumn("event");
			x1 = set.findColumn("min_x");
			z1 = set.findColumn("min_z");
			x2 = set.findColumn("max_x");
			z2 = set.findColumn("max_z");
			adjust = set.findColumn("adjust_coordinates");
			transX = set.findColumn("translate_x");
			transZ = set.findColumn("translate_z");
		}
		
	}
	
	private static class ObjectInformation {
		
		private final ColumnIndexes index;
		private final ResultSet set;
		private final Location l;
		private long id;
		private boolean snapshot;
		private String template;
		private float radius;
		private long container;
		private int cell;
		
		public ObjectInformation(ResultSet set) throws SQLException {
			this.set = set;
			index = new ColumnIndexes(set);
			l = new Location();
		}
		
		public void load(CrcDatabase strings) throws SQLException {
			id = set.getLong(index.idInd);
			snapshot = set.getBoolean(index.snapInd);
			template = strings.getString(set.getInt(index.crcInd));
			l.setPosition(set.getDouble(index.xInd), set.getDouble(index.yInd), set.getDouble(index.zInd));
			l.setOrientation(set.getDouble(index.oxInd), set.getDouble(index.oyInd), set.getDouble(index.ozInd), set.getDouble(index.owInd));
			radius = set.getInt(index.radiusInd);
			container = set.getLong(index.contInd);
			cell = set.getInt(index.cellInd);
		}
		
		public int getAreaIdNoLoad() throws SQLException {
			return set.getInt(index.areaInd);
		}
		
		public long getId() { return id; }
		public boolean isSnapshot() { return snapshot; }
		public String getTemplate() { return template; }
		public Location getLocation() { return l; }
		public double getRadius() { return radius; }
		public long getContainer() { return container; }
		public int getCell() { return cell; }
	}
	
	private static class ColumnIndexes {
		
		public final int idInd;
		public final int snapInd;
		public final int crcInd;
		public final int areaInd;
		public final int xInd;
		public final int yInd;
		public final int zInd;
		public final int oxInd;
		public final int oyInd;
		public final int ozInd;
		public final int owInd;
		public final int radiusInd;
		public final int contInd;
		public final int cellInd;
		
		public ColumnIndexes(ResultSet set) throws SQLException {
			idInd = set.findColumn("id");
			snapInd = set.findColumn("snapshot");
			crcInd = set.findColumn("template_crc");
			areaInd = set.findColumn("area_id");
			xInd = set.findColumn("x");
			yInd = set.findColumn("y");
			zInd = set.findColumn("z");
			oxInd = set.findColumn("orientation_x");
			oyInd = set.findColumn("orientation_y");
			ozInd = set.findColumn("orientation_z");
			owInd = set.findColumn("orientation_w");
			radiusInd = set.findColumn("radius");
			contInd = set.findColumn("container_id");
			cellInd = set.findColumn("cell_index");
		}
		
	}
	
}
