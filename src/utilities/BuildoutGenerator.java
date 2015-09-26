package utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import resources.Location;
import resources.Quaternion;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.CrcStringTableData;
import resources.client_info.visitors.DatatableData;
import resources.objects.SWGObject;
import resources.objects.buildouts.BuildoutArea;
import resources.objects.buildouts.BuildoutLoader;
import resources.objects.buildouts.SnapshotLoader;
import resources.objects.cell.CellObject;

public class BuildoutGenerator {
	
	private static final CrcStringTableData CRC_TABLE = (CrcStringTableData) ClientFactory.getInfoFromFile("misc/object_template_crc_string_table.iff");
	
	private static final String floatType = "REAL NOT NULL";
	private static final String intType = "INTEGER NOT NULL";
	private static final String strType = "TEXT NOT NULL";
	
	private final List<GenBuildoutArea> areas;
	
	public static void main(String [] args) throws IOException {
		BuildoutGenerator gen = new BuildoutGenerator();
		gen.createBuildoutSdb(new File("areas.sdb"), new File("objects.sdb"));
	}
	
	public BuildoutGenerator() {
		areas = new ArrayList<>();
	}
	
	public void createBuildoutSdb(File areaFile, File objectFile) throws IOException {
		createAreas(areaFile);
		createObjects(objectFile);
	}
	
	private void createAreas(File areaFile) throws IOException {
		SdbGenerator gen = new SdbGenerator(areaFile);
		gen.open();
		gen.setColumnNames("id", "terrain", "area_name", "event", "min_x", "min_z", "max_x", "max_z", "adjust_coordinates");
		gen.setColumnTypes("INTEGER PRIMARY KEY", strType, strType, strType+" DEFAULT \"\"", floatType, floatType, floatType, floatType, "INTEGER NOT NULL DEFAULT 0");
		writeAreas(gen);
		gen.close();
	}
	
	private void createObjects(File objectFile) throws IOException {
		SdbGenerator gen = new SdbGenerator(objectFile);
		gen.open();
		gen.setColumnNames("id", "buildout_id", "area_id", "templateCrc", "containerId", "x", "y", "z", "orientation_x", "orientation_y", "orientation_z", "orientation_w", "radius", "cellIndex");
		gen.setColumnTypes("INTEGER PRIMARY KEY", intType, intType, intType, intType, floatType, floatType, floatType, floatType, floatType, floatType, floatType, floatType, intType);
		BuildoutLoader loader = new BuildoutLoader();
		loader.loadAllBuildouts();
		List<SWGObject> objects = new ArrayList<>();
		for (SWGObject obj : loader.getObjects()) {
			addClientObject(objects, obj);
		}
		SnapshotLoader snapLoader = new SnapshotLoader();
		snapLoader.loadAllSnapshots();
		List<SWGObject> snapshots = new LinkedList<>();
		for (SWGObject obj : snapLoader.getObjects()) {
			addClientObject(snapshots, obj);
		}
		for (SWGObject snap : snapshots) {
			GenBuildoutArea area = getAreaForObject(snap);
			if (area != null)
				snap.setAreaId(area.id);
		}
		objects.addAll(snapshots);
		long buildoutId = 1;
		for (SWGObject obj : objects)
			writeObject(gen, obj, buildoutId++);
		gen.close();
	}
	
	private void addClientObject(List<SWGObject> objects, SWGObject obj) {
		objects.add(obj);
		for (SWGObject child : obj.getContainedObjects()) {
			addClientObject(objects, child);
		}
		for (SWGObject child : obj.getSlots().values()) {
			if (child != null)
				addClientObject(objects, child);
		}
	}
	
	private void writeAreas(SdbGenerator gen) throws IOException {
		DatatableData table = (DatatableData) ClientFactory.getInfoFromFile("datatables/buildout/buildout_scenes.iff");
		areas.clear();
		for (int sceneRow = 0; sceneRow < table.getRowCount(); sceneRow++) {
			Terrain t = Terrain.getTerrainFromName((String) table.getCell(sceneRow, 0));
			getArea(t, sceneRow, (Boolean) table.getCell(sceneRow, 1));
		}
		Collections.sort(areas);
		for (int i = 0; i < areas.size(); i++) {
			GenBuildoutArea area = areas.get(i);
			writeArea(gen, area, null);
			for (int j = i+1; j < areas.size() && area.equals(areas.get(j)); j++) {
				writeArea(gen, areas.get(j), area.area.getName());
				i++;
			}
		}
	}
	
	private void getArea(Terrain t, int sceneRow, boolean adjust) throws IOException {
		String file = "datatables/buildout/areas_"+t.getName()+".iff";
		DatatableData areaTable = (DatatableData) ClientFactory.getInfoFromFile(file);
		for (int row = 0; row < areaTable.getRowCount(); row++) {
			BuildoutArea area = new BuildoutArea();
			area.load(areaTable.getRow(row), sceneRow, row);
			areas.add(new GenBuildoutArea(area, t, area.getX1(), area.getZ1(), area.getX2(), area.getZ2(), sceneRow*100+row, adjust));
		}
	}
	
	private void writeArea(SdbGenerator gen, GenBuildoutArea area, String substituteName) throws IOException {
		if (substituteName == null)
			substituteName = area.area.getName();
		String terrain = area.terrain.getName();
		double x1 = area.area.getX1();
		double z1 = area.area.getZ1();
		double x2 = area.area.getX2();
		double z2 = area.area.getZ2();
		gen.writeLine(area.id, terrain, substituteName, area.area.getEventRequired(), x1, z1, x2, z2, area.adjust?"1":"0");
	}
	
	private void writeObject(SdbGenerator gen, SWGObject object, long buildoutId) throws IOException {
		long id = object.getObjectId();
		int crc = CRC_TABLE.getCrcForString(object.getTemplate());
		long container = (object.getParent() != null) ? object.getParent().getObjectId() : 0;
		Location l = object.getLocation();
		Quaternion q = l.getOrientation();
		double radius = object.getLoadRange();
		int cellIndex = (object instanceof CellObject) ? ((CellObject) object).getNumber() : 0;
		gen.writeLine(id, buildoutId, object.getAreaId(), crc, container, l.getX(), l.getY(), l.getZ(), q.getX(), q.getY(), q.getZ(), q.getW(), radius, cellIndex);
	}
	
	private GenBuildoutArea getAreaForObject(SWGObject obj) {
		Location l = obj.getWorldLocation();
		double x = l.getX();
		double z = l.getZ();
		int ind = Collections.binarySearch(areas, new GenBuildoutArea(null, obj.getTerrain(), x, z, x, z, 0, false), (area1, area2) -> {
			int comp = area1.terrain.getName().compareTo(area2.terrain.getName());
			if (comp != 0)
				return comp;
			if (area2.x1 < area1.x1)
				return 1;
			if (area2.x2 > area1.x2)
				return -1;
			if (area2.z1 < area1.z1)
				return 1;
			if (area2.z2 > area1.z2)
				return -1;
			return Integer.compare(area1.getSorting(), area2.getSorting());
		});
		if (ind < 0)
			return null;
		return areas.get(ind);
	}
	
	private static class GenBuildoutArea implements Comparable<GenBuildoutArea> {
		public final BuildoutArea area;
		public final Terrain terrain;
		public final int x1;
		public final int z1;
		public final int x2;
		public final int z2;
		public final int id;
		public final boolean adjust;
		
		public GenBuildoutArea(BuildoutArea area, Terrain terrain, double x1, double z1, double x2, double z2, int id, boolean adjust) {
			this.area = area;
			this.terrain = terrain;
			this.x1 = (int) x1;
			this.z1 = (int) z1;
			this.x2 = (int) x2;
			this.z2 = (int) z2;
			this.id = id;
			this.adjust = adjust;
		}
		
		public int compareTo(GenBuildoutArea area) {
			int comp = terrain.getName().compareTo(area.terrain.getName());
			if (comp != 0)
				return comp;
			comp = Integer.compare(x1, area.x1);
			if (comp != 0)
				return comp;
			comp = Integer.compare(z1, area.z1);
			if (comp != 0)
				return comp;
			comp = Integer.compare(x2, area.x2);
			if (comp != 0)
				return comp;
			comp = Integer.compare(z2, area.z2);
			if (comp != 0)
				return comp;
			return Integer.compare(getSorting(), area.getSorting());
		}
		
		public boolean equals(Object o) {
			if (o == null || !(o instanceof GenBuildoutArea))
				return false;
			GenBuildoutArea area = (GenBuildoutArea) o;
			if (!terrain.equals(area.terrain))
				return false;
			if (x1 != area.x1)
				return false;
			if (z1 != area.z1)
				return false;
			return true;
		}
		
		public int hashCode() {
			return terrain.hashCode() ^ Integer.hashCode(x1) ^ Integer.hashCode(z1);
		}
		
		private int getSorting() {
			if (area == null)
				return 1;
			String [] parts = area.getName().split("_");
			if (parts.length != 3)
				return 2;
			if (isNumber(parts[1]) && isNumber(parts[2]))
				return 1;
			return 2;
		}
		
		private boolean isNumber(String str) {
			try {
				Integer.parseInt(str);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
	
}
