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
package utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Quaternion;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;

import resources.objects.SWGObject;
import resources.objects.buildouts.BuildoutLoader;
import resources.objects.buildouts.SnapshotLoader;
import resources.objects.buildouts.SwgBuildoutArea;
import resources.objects.cell.CellObject;

public class BuildoutGenerator {
	
	private static final String floatType = "REAL NOT NULL";
	private static final String intType = "INTEGER NOT NULL";
	private static final String strType = "TEXT NOT NULL";
	private static final String [][] AREA_COLUMNS = {
		{"id", "terrain", "area_name", "event", "min_x", "min_z", "max_x", "max_z", "adjust_coordinates", "translate_x", "translate_z"},
		{"INTEGER PRIMARY KEY", strType, strType, strType+" DEFAULT \"\"", floatType, floatType, floatType, floatType, "INTEGER NOT NULL DEFAULT 0", floatType, floatType}
	};
	private static final String [][] OBJECT_COLUMNS = {
		{"id", "snapshot", "area_id", "template_crc", "container_id", "x", "y", "z", "orientation_x", "orientation_y", "orientation_z", "orientation_w", "radius", "cell_index"},
		{"INTEGER PRIMARY KEY", intType, intType, intType, intType, floatType, floatType, floatType, floatType, floatType, floatType, floatType, floatType, intType}
	};
	
	private final List<GenBuildoutArea> areas;
	private final List<GenBuildoutArea> fallbackAreas;
	
	public static void main(String [] args) throws IOException {
		BuildoutGenerator gen = new BuildoutGenerator();
		File areas = new File("serverdata/buildout/areas.sdb");
		File objects = new File("serverdata/buildout/objects.sdb");
		gen.createBuildoutSdb(areas, objects);
		System.exit(0);
	}
	
	public BuildoutGenerator() { // 118114
		areas = new ArrayList<>();
		fallbackAreas = new ArrayList<>(Terrain.values().length);
		int areaId = -1;
		for (Terrain t : Terrain.values()) {
			fallbackAreas.add(new GenBuildoutArea(null, t, -8196, -8196, 8196, 8196, areaId, false));
			areaId--;
		}
	}
	
	public void createBuildoutSdb(File areaFile, File objectFile) throws IOException {
		createAreas(areaFile);
		createObjects(objectFile);
	}
	
	private void createAreas(File areaFile) throws IOException {
		System.out.println("Generating areas...");
		SdbGenerator gen = new SdbGenerator(areaFile);
		gen.open();
		gen.setColumnNames(AREA_COLUMNS[0]);
		gen.setColumnTypes(AREA_COLUMNS[1]);
		writeAreas(gen);
		gen.close();
	}
	
	private void createObjects(File objectFile) throws IOException {
		System.out.println("Generating objects...");
		List<SWGObject> objects = new LinkedList<>();
		System.out.println("    Creating buildouts...");
		createBuildouts(objects);
		System.out.println("    Creating snapshots...");
		createSnapshots(objects);
		System.out.println("    Generating file...");
		generateObjectFile(objectFile, objects);
	}
	
	private void createBuildouts(List <SWGObject> objects) {
		BuildoutLoader loader = new BuildoutLoader();
		loader.loadAllBuildouts();
		for (Entry<String, List<SWGObject>> areaObjects : loader.getObjects().entrySet()) {
			GenBuildoutArea area = null;
			for (GenBuildoutArea a : areas) {
				if (a.area.getName().equals(areaObjects.getKey())) {
					area = a;
					break;
				}
			}
			if (area == null) {
				System.err.println("Unknown area! Name: " + areaObjects.getKey());
				continue;
			}
			for (SWGObject obj : areaObjects.getValue()) {
				obj.setBuildoutAreaId(area.id);
				addClientObject(objects, obj);
			}
		}
	}
	
	private void createSnapshots(List<SWGObject> objects) {
		SnapshotLoader snapLoader = new SnapshotLoader();
		snapLoader.loadAllSnapshots();
		List<SWGObject> snapshots = new LinkedList<>();
		for (SWGObject obj : snapLoader.getObjects()) {
			addClientObject(snapshots, obj);
		}
		setSnapshotData(snapshots, objects);
	}
	
	private void setSnapshotData(List<SWGObject> snapshots, List<SWGObject> objects) {
		for (SWGObject snap : snapshots) {
			GenBuildoutArea area = getAreaForObject(snap);
			if (area != null)
				snap.setBuildoutAreaId(area.id);
			else
				System.err.println("No area for: " + snap.getObjectId() + " / " + snap.getTerrain());
		}
		objects.addAll(snapshots);
	}
	
	private void generateObjectFile(File objectFile, List<SWGObject> objects) throws IOException {
		try (SdbGenerator gen = new SdbGenerator(objectFile)) {
			gen.open();
			gen.setColumnNames(OBJECT_COLUMNS[0]);
			gen.setColumnTypes(OBJECT_COLUMNS[1]);
			int objNum = 0;
			int percent = 0;
			objects.sort((o1, o2) -> {
				int comp = Integer.compare(getBuildoutDepth(o1), getBuildoutDepth(o2));
				if (comp != 0)
					return comp;
				comp = Integer.compare(o1.getBuildoutAreaId(), o2.getBuildoutAreaId());
				if (comp != 0)
					return comp;
				comp = Long.compare(o1.getParent() == null ? 0 : o1.getParent().getObjectId(), o2.getParent() == null ? 0 : o2.getParent()
						.getObjectId());
				if (comp != 0)
					return comp;
				comp = Integer.compare(o1 instanceof CellObject ? ((CellObject) o1).getNumber() : 0, o2 instanceof CellObject ? ((CellObject) o2)
						.getNumber() : 0);
				if (comp != 0)
					return comp;
				return 0;
			});
			for (SWGObject obj : objects) {
				if (obj instanceof CellObject)
					continue;
				writeObject(gen, obj);
				while (percent / 100.0 * objects.size() <= objNum) {
					System.out.print(".");
					percent++;
				}
				objNum++;
			}
			System.out.println();
		}
	}
	
	private int getBuildoutDepth(SWGObject object) { 
		return getBuildoutDepth(object, 0);
	}
	
	private int getBuildoutDepth(SWGObject object, int depth) {
		if (object.getParent() == null)
			return depth;
		return getBuildoutDepth(object.getParent(), depth+1);
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
		for (int i = fallbackAreas.size()-1; i >= 0; i--) {
			GenBuildoutArea fallback = fallbackAreas.get(i);
			String name = fallback.terrain.name().toLowerCase(Locale.US);
			gen.writeLine(fallback.id, name, name + "_global", "", -8196, -8196, 8196, 8196, "0", 0, 0);
		}
		int percent = 0;
		for (int i = 0; i < areas.size(); i++) {
			GenBuildoutArea area = areas.get(i);
			writeArea(gen, area, null);
			for (int j = i+1; j < areas.size() && area.equals(areas.get(j)); j++) {
				writeArea(gen, areas.get(j), area.area.getName());
				i++;
			}
			while (percent / 100.0 * areas.size() <= i) {
				System.out.print(".");
				percent++;
			}
		}
		System.out.println();
	}
	
	private void getArea(Terrain t, int sceneRow, boolean adjust) {
		String file = "datatables/buildout/areas_"+t.getName()+".iff";
		DatatableData areaTable = (DatatableData) ClientFactory.getInfoFromFile(file);
		for (int row = 0; row < areaTable.getRowCount(); row++) {
			SwgBuildoutArea area = new SwgBuildoutArea();
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
		double transX = 0;
		double transZ = 0;
		boolean adjust = area.adjust || !area.area.getCompositeName().isEmpty();
		if (!area.area.getCompositeName().isEmpty()) {
			transX = area.area.getCompositeX1() + (area.area.getCompositeX2() - area.area.getCompositeX1()) / 2;
			transZ = area.area.getCompositeZ1() + (area.area.getCompositeZ2() - area.area.getCompositeZ1()) / 2;
		}
		transX -= area.area.getX1() + (area.area.getX2() - area.area.getX1()) / 2;
		transZ -= area.area.getZ1() + (area.area.getZ2() - area.area.getZ1()) / 2;
		gen.writeLine(area.id, terrain, substituteName, area.area.getEventRequired(), x1, z1, x2, z2, adjust?"1":"0", transX, transZ);
	}
	
	private void writeObject(SdbGenerator gen, SWGObject object) throws IOException {
		long id = object.getObjectId();
		int crc = CRC.getCrc(object.getTemplate());
		long container = (object.getParent() != null) ? object.getParent().getObjectId() : 0;
		if (object.getParent() instanceof CellObject)
			container = object.getParent().getParent().getObjectId();
		Location l = object.getLocation();
		Quaternion q = l.getOrientation();
		double radius = object.getLoadRange();
		int cellIndex = (object.getParent() instanceof CellObject) ? ((CellObject) object.getParent()).getNumber() : 0;
		int snapshot = object.isSnapshot() ? 1 : 0;
		float x = (float) l.getX(), y = (float) l.getY(), z = (float) l.getZ();
		float oX = (float) q.getX(), oY = (float) q.getY(), oZ = (float) q.getZ(), oW = (float) q.getW();
		gen.writeLine(id, snapshot, object.getBuildoutAreaId(), crc, container, x, y, z, oX, oY, oZ, oW, radius, cellIndex);
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
		if (ind < 0) {
			for (GenBuildoutArea fallback : fallbackAreas) {
				if (fallback.terrain == obj.getTerrain())
					return fallback;
			}
			return null;
		}
		return areas.get(ind);
	}
	
	private static class GenBuildoutArea implements Comparable<GenBuildoutArea> {
		public final SwgBuildoutArea area;
		public final Terrain terrain;
		public final int index;
		public final int x1;
		public final int z1;
		public final int x2;
		public final int z2;
		public final int id;
		public final boolean adjust;
		
		public GenBuildoutArea(SwgBuildoutArea area, Terrain terrain, double x1, double z1, double x2, double z2, int id, boolean adjust) {
			this.area = area;
			this.terrain = terrain;
			if (area != null)
				this.index = area.getIndex();
			else
				this.index = -1;
			this.x1 = (int) x1;
			this.z1 = (int) z1;
			this.x2 = (int) x2;
			this.z2 = (int) z2;
			this.id = id;
			this.adjust = adjust;
		}
		
		@Override
		public int compareTo(GenBuildoutArea area) {
			int comp = Integer.compare(index, area.index);
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
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof GenBuildoutArea))
				return false;
			GenBuildoutArea area = (GenBuildoutArea) o;
			return terrain.equals(area.terrain) && x1 == area.x1 && z1 == area.z1;
		}
		
		@Override
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
