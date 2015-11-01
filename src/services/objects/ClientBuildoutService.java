package services.objects;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import intents.player.PlayerTransformedIntent;
import resources.Location;
import resources.Terrain;
import resources.buildout.BuildoutArea;
import resources.buildout.BuildoutArea.BuildoutAreaBuilder;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.CrcStringTableData;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.server_info.Config;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class ClientBuildoutService extends Service {
	
	private static final String GET_BUILDOUT_AREAS = "SELECT * FROM areas ORDER BY area_name ASC, event ASC";
	private static final String GET_CLIENT_OBJECTS_SQL = "SELECT objects.*, areas.terrain "
			+ "FROM objects, areas "
			+ "WHERE objects.area_id = areas.id "
			+ "ORDER BY buildout_id ASC";
	
	private final CrcStringTableData strings;
	private final RelationalServerData clientSdb;
	private final List<BuildoutArea> areas;
	private final Map<Integer, BuildoutArea> areasById;
	private final PreparedStatement getClientObjects;
	
	public ClientBuildoutService() {
		strings = (CrcStringTableData) ClientFactory.getInfoFromFile("misc/object_template_crc_string_table.iff");
		clientSdb = RelationalServerFactory.getServerData("buildout/buildouts.db", "areas", "objects");
		if (clientSdb == null)
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for ClientObjectLoader");
		areas = new ArrayList<>();
		areasById = new Hashtable<>(1000); // Number of buildout areas
		
		getClientObjects = clientSdb.prepareStatement(GET_CLIENT_OBJECTS_SQL);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(PlayerTransformedIntent.TYPE);
		List<String> events = getEvents();
		loadAreas(events);
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case PlayerTransformedIntent.TYPE:
				if (i instanceof PlayerTransformedIntent)
					handlePlayerTransform((PlayerTransformedIntent) i);
				break;
			default:
				break;
		}
	}
	
	public Map<Long, SWGObject> loadClientObjects() {
		Config c = getConfig(ConfigFile.PRIMARY);
		if (c.getBoolean("LOAD-OBJECTS", true)) {
			System.out.println("ClientBuildoutService: Loading client objects...");
			Log.i("ClientBuildoutService", "Loading client objects...");
			long startLoad = System.nanoTime();
			Map<Long, SWGObject> objects = new Hashtable<>(4*1024);
			loadClientObjects(objects);
			double loadTime = (System.nanoTime() - startLoad) / 1E6;
			System.out.printf("ClientObjectLoader: Finished loading %d client objects. Time: %fms%n", objects.size(), loadTime);
			Log.i("ClientObjectLoader", "Finished loading %d client objects. Time: %fms", objects.size(), loadTime);
			return objects;
		} else {
			Log.w("ClientObjectLoader", "Did not load client objects. Reason: Disabled.");
			System.out.println("ClientObjectLoader: Did not load client objects. Reason: Disabled!");
		}
		return new HashMap<>();
	}
	
	private void loadClientObjects(Map<Long, SWGObject> objects) {
		try (ResultSet set = getClientObjects.executeQuery()) {
			set.setFetchSize(1500);
			BuildoutArea area = null;
			ColumnIndexes ind = new ColumnIndexes(set);
			SWGObject obj;
			Location l = new Location();
			while (set.next()) {
				area = areasById.get(ind.areaInd);
				obj = createObject(set, objects, l, area, ind);
				objects.put(obj.getObjectId(), obj);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
	
	private SWGObject createObject(ResultSet set, Map<Long, SWGObject> objects, Location l, BuildoutArea area, ColumnIndexes ind) throws SQLException {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getLong(ind.idInd), strings.getTemplateString(set.getInt(ind.crcInd)));
		l.setTerrain(Terrain.getTerrainFromName(set.getString(ind.terInd)));
		l.setPosition(set.getDouble(ind.xInd), set.getDouble(ind.yInd), set.getDouble(ind.zInd));
		l.setOrientation(set.getDouble(ind.oxInd), set.getDouble(ind.oyInd), set.getDouble(ind.ozInd), set.getDouble(ind.owInd));
		obj.setLocation(l);
		obj.setBuildout(true);
		obj.setBuildoutArea(area);
		obj.setLoadRange(set.getInt(ind.radiusInd));
		int cell = set.getInt(ind.cellInd);
		if (cell != 0 && obj instanceof CellObject)
			((CellObject) obj).setNumber(cell);
		long container = set.getLong(ind.contInd);
		if (container != 0)
			objects.get(container).addObject(obj);
		return obj;
	}
	
	private void loadAreas(List <String> events) {
		BuildoutArea primary = null; // Stored as "best area" for what we want to load
		try (RelationalServerData data = RelationalServerFactory.getServerData("buildout/buildouts.db", "areas")) {
			try (ResultSet set = data.prepareStatement(GET_BUILDOUT_AREAS).executeQuery()) {
				areas.clear();
				areasById.clear();
				AreaIndexes ind = new AreaIndexes(set);
				boolean loaded = false;
				while (set.next()) {
					BuildoutArea area = createArea(set, ind);
					if (area.getEvent().isEmpty() && (primary == null || !area.getName().equals(primary.getName()))) {
						if (!loaded && primary != null)
							loadArea(primary);
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
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void handlePlayerTransform(PlayerTransformedIntent pti) {
		CreatureObject creature = pti.getPlayer();
		Location world = creature.getWorldLocation();
		BuildoutArea area = creature.getBuildoutArea();
		if (area == null || compare(area, world.getTerrain(), world.getX(), world.getZ()) != 0) {
			area = getAreaForObject(creature);
			creature.setBuildoutArea(area);
		}
	}
	
	private void loadArea(BuildoutArea area) {
		areas.add(area);
		areasById.put(area.getId(), area);
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
			.setAdjustCoordinates(set.getBoolean(ind.adjust));
		return bldr.build();
	}
	
	private BuildoutArea getAreaForObject(SWGObject obj) {
		Location l = obj.getWorldLocation();
		return binarySearch(l.getTerrain(), l.getX(), l.getZ(), 0, areas.size());
	}
	
	private BuildoutArea binarySearch(Terrain t, double x, double z, int low, int high) {
		int mid = mid(low, high);
		BuildoutArea midArea = areas.get(mid);
		int comp = compare(midArea, t, x, z);
		if (comp == 0)
			return midArea;
		if (low == mid)
			return null;
		if (comp < 0)
			return binarySearch(t, x, z, low, mid);
		return binarySearch(t, x, z, mid, high);
	}
	
	private int compare(BuildoutArea cur, Terrain t, double x, double z) {
		int comp = cur.getTerrain().getName().compareTo(t.getName());
		if (comp != 0)
			return comp;
		if (x < cur.getX1())
			return 1;
		if (x > cur.getX2())
			return -1;
		if (z < cur.getZ1())
			return 1;
		if (z > cur.getZ2())
			return -1;
		return 0;
	}
	
	private int mid(int low, int high) {
		return (low + high) / 2;
	}
	
	private static class AreaIndexes {
		
		private int id;
		private int name;
		private int terrain;
		private int event;
		private int x1, z1, x2, z2;
		private int adjust;
		
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
		}
		
	}
	
	private static class ColumnIndexes {
		
		public final int idInd;
		public final int crcInd;
		public final int terInd;
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
			crcInd = set.findColumn("templateCrc");
			terInd = set.findColumn("terrain");
			areaInd = set.findColumn("area_id");
			xInd = set.findColumn("x");
			yInd = set.findColumn("y");
			zInd = set.findColumn("z");
			oxInd = set.findColumn("orientation_x");
			oyInd = set.findColumn("orientation_y");
			ozInd = set.findColumn("orientation_z");
			owInd = set.findColumn("orientation_w");
			radiusInd = set.findColumn("radius");
			contInd = set.findColumn("containerId");
			cellInd = set.findColumn("cellIndex");
		}
		
	}
	
}
