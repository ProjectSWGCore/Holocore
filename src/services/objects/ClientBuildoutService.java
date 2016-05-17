package services.objects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
		return Collections.unmodifiableCollection(objects.values());
	}
	
	private void createObject(Map<Long, SWGObject> objects, BuildoutArea area, ObjectInformation info) throws SQLException {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(info.getId(), info.getTemplate());
		Location l = info.getLocation();
		l.setTerrain(area.getTerrain());
		obj.setLocation(l);
		obj.setClassification(info.isSnapshot() ? ObjectClassification.SNAPSHOT : ObjectClassification.BUILDOUT);
		obj.setBuildoutArea(area);
		obj.setLoadRange(info.getRadius());
		checkCell(obj, info.getCell());
		checkChild(objects, obj, info.getContainer());
		objects.put(obj.getObjectId(), obj);
	}
	
	private void checkCell(SWGObject obj, int cell) {
		if (cell != 0 && obj instanceof CellObject)
			((CellObject) obj).setNumber(cell);
	}
	
	private void checkChild(Map<Long, SWGObject> objects, SWGObject obj, long container) {
		if (container != 0)
			objects.get(container).addObject(obj);
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
