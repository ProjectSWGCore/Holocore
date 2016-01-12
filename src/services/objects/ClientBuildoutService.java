package services.objects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
	private static final String GET_CLIENT_OBJECTS_SQL = "SELECT objects.id, objects.area_id, objects.templateCrc, objects.containerId, "
			+ "objects.x, objects.y, objects.z, objects.orientation_x, objects.orientation_y, objects.orientation_z, objects.orientation_w, "
			+ "objects.radius, objects.cellIndex "
			+ "FROM objects "
			+ "ORDER BY buildout_depth, area_id ASC";
	
	private final CrcStringTableData strings;
	private final List<BuildoutArea> areas;
	private final Map<Integer, BuildoutArea> areasById;
	
	public ClientBuildoutService() {
		strings = (CrcStringTableData) ClientFactory.getInfoFromFile("misc/object_template_crc_string_table.iff");
		areas = new ArrayList<>();
		areasById = new Hashtable<>(2100); // Number of buildout areas
		
		registerForIntent(PlayerTransformedIntent.TYPE);
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
	
	public void loadClientObjects() {
		try (RelationalServerData clientSdb = RelationalServerFactory.getServerData("buildout/buildouts.db", "areas", "objects")) {
			loadAreas(clientSdb, getEvents());
			Config c = getConfig(ConfigFile.PRIMARY);
			if (c.getBoolean("LOAD-OBJECTS", true)) {
				System.out.println("ClientBuildoutService: Loading client objects...");
				Log.i("ClientBuildoutService", "Loading client objects...");
				long startLoad = System.nanoTime();
				int objects = loadObjects(clientSdb);
				double loadTime = (System.nanoTime() - startLoad) / 1E6;
				System.out.printf("ClientObjectLoader: Finished loading %d client objects. Time: %fms%n", objects, loadTime);
				Log.i("ClientObjectLoader", "Finished loading %d client objects. Time: %fms", objects, loadTime);
			} else {
				Log.w("ClientObjectLoader", "Did not load client objects. Reason: Disabled.");
				System.out.println("ClientObjectLoader: Did not load client objects. Reason: Disabled!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Log.e(this, e);
		}
	}
	
	private int loadObjects(RelationalServerData data) {
		int count = 0;
		try (ResultSet set = data.executeQuery(GET_CLIENT_OBJECTS_SQL)) {
			BuildoutArea area = null;
			Map<Long, SWGObject> objects = new Hashtable<>();
			ObjectInformation info = new ObjectInformation(set);
			while (set.next()) {
				area = areasById.get(info.getAreaIdNoLoad());
				if (!area.isLoaded())
					continue;
				info.load(strings);
				new ObjectCreatedIntent(createObject(objects, area, info)).broadcast();
				count++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Log.e(this, e);
		}
		return count;
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
	
	private SWGObject createObject(Map<Long, SWGObject> objects, BuildoutArea area, ObjectInformation info) throws SQLException {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(info.getId(), info.getTemplate());
		Location l = info.getLocation();
		l.setTerrain(area.getTerrain());
		obj.setLocation(l);
		obj.setBuildout(true);
		obj.setBuildoutArea(area);
		obj.setLoadRange(info.getRadius());
		int cell = info.getCell();
		if (cell != 0 && obj instanceof CellObject)
			((CellObject) obj).setNumber(cell);
		long container = info.getContainer();
		if (container != 0)
			objects.get(container).addObject(obj);
		objects.put(obj.getObjectId(), obj);
		return obj;
	}
	
	private void loadAreas(RelationalServerData data, List <String> events) throws SQLException {
		BuildoutArea primary = null; // Stored as "best area" for what we want to load
		try (ResultSet set = data.executeQuery(GET_BUILDOUT_AREAS)) {
			areas.clear();
			areasById.clear();
			AreaIndexes ind = new AreaIndexes(set);
			boolean loaded = false;
			while (set.next()) {
				BuildoutArea area = createArea(set, ind);
				area.setLoaded(false);
				areas.add(area);
				areasById.put(area.getId(), area);
				if (area.getEvent().isEmpty() && (primary == null || !area.getName().equals(primary.getName()))) {
					if (!loaded && primary != null)
						area.setLoaded(true);
					loaded = false;
					primary = area; // Primary area, no event
				}
				if (events.contains(area.getEvent())) {
					area.setLoaded(true);
					loaded = true;
				}
			}
			if (!loaded && primary != null)
				primary.setLoaded(true);
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
	
	private static class ObjectInformation {
		
		private final ColumnIndexes index;
		private final ResultSet set;
		private final Location l;
		private long id;
		private String template;
		private int radius;
		private long container;
		private int cell;
		
		public ObjectInformation(ResultSet set) throws SQLException {
			this.set = set;
			index = new ColumnIndexes(set);
			l = new Location();
		}
		
		public void load(CrcStringTableData strings) throws SQLException {
			id = set.getLong(index.idInd);
			template = strings.getTemplateString(set.getInt(index.crcInd));
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
		public String getTemplate() { return template; }
		public Location getLocation() { return l; }
		public double getRadius() { return radius; }
		public long getContainer() { return container; }
		public int getCell() { return cell; }
	}
	
	private static class ColumnIndexes {
		
		public final int idInd;
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
			crcInd = set.findColumn("templateCrc");
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
