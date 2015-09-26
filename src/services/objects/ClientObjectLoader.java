package services.objects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import resources.Location;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.CrcStringTableData;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.cell.CellObject;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;

public class ClientObjectLoader {
	
	private static final String FILE_PREFIX = "serverdata/buildout/";
	private static final String GET_CLIENT_OBJECTS_SQL = "SELECT objects.*, areas.adjust_coordinates, areas.terrain "
			+ "FROM objects, areas "
			+ "WHERE areas.event == '' AND objects.area_id = areas.id "
			+ "ORDER BY buildout_id ASC";
	
	private final CrcStringTableData strings;
	private final RelationalServerData clientSdb;
	
	public ClientObjectLoader() {
		strings = (CrcStringTableData) ClientFactory.getInfoFromFile("misc/object_template_crc_string_table.iff");
		clientSdb = new RelationalServerData(FILE_PREFIX+"buildouts.db");
	}
	
	public Map<Long, SWGObject> loadClientObjects(Terrain terrain) {
		long startLoad = System.nanoTime();
		System.out.println("ClientObjectLoader: Loading client objects...");
		Log.i("ClientObjectLoader", "Loading client objects...");
		Map<Long, SWGObject> objects = new Hashtable<>(4*1024);
		if (!loadTables())
			return objects;
		long loaded = 0;
		try (ResultSet set = clientSdb.prepareStatement(GET_CLIENT_OBJECTS_SQL).executeQuery()) {
			set.setFetchSize(1500);
			ColumnIndexes ind = new ColumnIndexes(set);
			SWGObject obj;
			Location l = new Location();
			while (set.next()) {
				obj = createObject(set, objects, l, ind);
				objects.put(obj.getObjectId(), obj);
				loaded++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		System.out.printf("ClientObjectLoader: Finished loading %d client objects. Time: %fms%n", loaded, loadTime);
		Log.i("ClientObjectLoader", "Finished loading client objects. Time: %fms", loadTime);
		return objects;
	}
	
	private SWGObject createObject(ResultSet set, Map<Long, SWGObject> objects, Location l, ColumnIndexes ind) throws SQLException {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(set.getLong(ind.idInd), strings.getTemplateString(set.getInt(ind.crcInd)));
		l.setTerrain(Terrain.getTerrainFromName(set.getString(ind.terInd)));
		l.setPosition(set.getDouble(ind.xInd), set.getDouble(ind.yInd), set.getDouble(ind.zInd));
		l.setOrientation(set.getDouble(ind.oxInd), set.getDouble(ind.oyInd), set.getDouble(ind.ozInd), set.getDouble(ind.owInd));
		obj.setLocation(l);
		obj.setLoadRange(set.getInt(ind.radiusInd));
		int cell = set.getInt(ind.cellInd);
		if (cell != 0 && obj instanceof CellObject)
			((CellObject) obj).setNumber(cell);
		long container = set.getLong(ind.contInd);
		if (container != 0)
			objects.get(container).addObject(obj);
		return obj;
	}
	
	private boolean loadTables() {
		if (!clientSdb.linkTableWithSdb("areas", FILE_PREFIX+"areas.sdb") || !clientSdb.linkTableWithSdb("objects", FILE_PREFIX+"objects.sdb")) {
			System.err.println("ObjectManager: Failed to load client objects! Cannot link SDB");
			Log.e("ObjectManager", "Failed to load client object SDB files!");
			return false;
		}
		return true;
	}
	
	private static class ColumnIndexes {
		
		public final int idInd;
		public final int crcInd;
		public final int terInd;
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
