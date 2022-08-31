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
package com.projectswg.utility.clientdata;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Quaternion;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.utilities.SdbGenerator;
import com.projectswg.utility.clientdata.buildouts.BuildoutLoader;
import com.projectswg.utility.clientdata.buildouts.SnapshotLoader;
import com.projectswg.utility.clientdata.buildouts.SwgBuildoutArea;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({ "UseOfSystemOutOrSystemErr", "CallToPrintStackTrace" }) // Converter class that's not meant to be used by Holocore
public class ConvertBuildouts implements Converter{
	
	private static final String [][] AREA_COLUMNS = {
		{"id", "terrain", "area_name", "min_x", "min_z", "max_x", "max_z", "adjust_coordinates", "translate_x", "translate_z"}
	};
	private static final String [][] OBJECT_COLUMNS = {
		{"id", "template_crc", "container_id", "event", "terrain", "x", "y", "z", "orientation_x", "orientation_y", "orientation_z", "orientation_w", "cell_index", "tag"}
	};
	
	private final List<GenBuildoutArea> areas;
	private final List<GenBuildoutArea> fallbackAreas;
	private final Map<Long, String> objectTagMap;
	private final BuildoutLoader buildoutLoader;
	private final SnapshotLoader snapshotLoader;
	
	public ConvertBuildouts() {
		this.areas = new ArrayList<>();
		this.fallbackAreas = new ArrayList<>(Terrain.values().length);
		this.objectTagMap = new HashMap<>();
		AtomicLong objectIdGenerator = new AtomicLong(1);
		AtomicLong cellIdGenerator = new AtomicLong(1000000);
		this.buildoutLoader = new BuildoutLoader(objectIdGenerator, cellIdGenerator);
		this.snapshotLoader = new SnapshotLoader(objectIdGenerator, cellIdGenerator);
		int areaId = -1;
		for (Terrain t : Terrain.values()) {
			fallbackAreas.add(new GenBuildoutArea(null, t, -8196, -8196, 8196, 8196, areaId, false));
			areaId--;
		}
	}
	
	@Override
	public void convert() {
		try {
			createAreas();
			createObjects();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createAreas() throws IOException {
		System.out.println("Generating areas...");
		try (SdbGenerator gen = new SdbGenerator(new File("serverdata/buildout/areas.sdb"))) {
			gen.writeColumnNames(AREA_COLUMNS[0]);
			writeAreas(gen);
		}
	}
	
	private void createObjects() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/buildout/object_tag_map.sdb"))) {
			while (set.next()) {
				objectTagMap.put(set.getInt(1), set.getText(0));
			}
		}
		System.out.println("Generating objects...");
		List<SWGObject> objects = new LinkedList<>();
		System.out.println("    Creating buildouts...");
		createBuildouts(objects);
		System.out.println("    Creating snapshots...");
		createSnapshots(objects);
		System.out.println("    Generating file...");
		generateObjectFile(objects);
	}
	
	private void createBuildouts(List <SWGObject> objects) {
		buildoutLoader.loadAllBuildouts();
		objects.addAll(buildoutLoader.getObjects());
	}
	
	private void createSnapshots(List<SWGObject> objects) {
		snapshotLoader.loadAllSnapshots();
		for (SWGObject snap : snapshotLoader.getObjects()) {
			GenBuildoutArea area = fallbackAreas.stream().filter(a -> a.terrain.equals(snap.getTerrain())).findFirst().orElse(null);
			if (area != null)
				snap.setBuildoutAreaId(area.id);
			else
				System.err.println("No area for: " + snap.getObjectId() + " / " + snap.getTerrain());
			objects.add(snap);
		}
	}
	
	private void generateObjectFile(List<SWGObject> objects) throws IOException {
		isValidBuildoutAreas(objects);
		try (SdbGenerator gen = new SdbGenerator(new File("serverdata/buildout/objects.sdb"))) {
			gen.writeColumnNames(OBJECT_COLUMNS[0]);
			int objNum = 0;
			int percent = 0;
			objects.sort(Comparator.comparingLong(SWGObject::getObjectId));
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
	
	private void writeAreas(SdbGenerator gen) throws IOException {
		areas.clear();
		{
			DatatableData table = (DatatableData) ClientFactory.getInfoFromFile("datatables/buildout/buildout_scenes.iff");
			int areaId = 1;
			for (int sceneRow = 0; sceneRow < table.getRowCount(); sceneRow++) {
				Terrain t = Terrain.getTerrainFromName((String) table.getCell(sceneRow, 0));
				boolean adjust = (Boolean) table.getCell(sceneRow, 1);
				
				String file = "datatables/buildout/areas_"+t.getName()+".iff";
				DatatableData areaTable = (DatatableData) ClientFactory.getInfoFromFile(file);
				for (int row = 0; row < areaTable.getRowCount(); row++) {
					SwgBuildoutArea area = new SwgBuildoutArea();
					area.load(areaTable.getRow(row), sceneRow, row);
					areas.add(new GenBuildoutArea(area, t, area.getX1(), area.getZ1(), area.getX2(), area.getZ2(), areaId, adjust));
					areaId++;
				}
			}
		}
		Collections.sort(areas);
		for (int i = fallbackAreas.size()-1; i >= 0; i--) {
			GenBuildoutArea fallback = fallbackAreas.get(i);
			String name = fallback.terrain.name().toLowerCase(Locale.US);
			gen.writeLine(fallback.id, name, name + "_global", -8196, -8196, 8196, 8196, "0", 0, 0);
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
		gen.writeLine(area.id, terrain, substituteName, x1, z1, x2, z2, adjust?"1":"0", transX, transZ);
	}
	
	private void writeObject(SdbGenerator gen, SWGObject object) throws IOException {
		long id = object.getObjectId();
		long originalId = buildoutLoader.getPreviousId(id);
		if (originalId == 0)
			originalId = snapshotLoader.getPreviousId(id);
		int crc = CRC.getCrc(object.getTemplate());
		long container = (object.getParent() != null) ? object.getParent().getObjectId() : 0;
		if (object.getParent() instanceof CellObject) {
			assert object.getParent().getParent() != null;
			container = object.getParent().getParent().getObjectId();
		}
		String event = object.getBuildoutEvent();
		Location l = object.getLocation();
		Quaternion q = l.getOrientation();
		int cellIndex = (object.getParent() instanceof CellObject) ? ((CellObject) object.getParent()).getNumber() : 0;
		Terrain terrain = l.getTerrain();
		float x = (float) l.getX(), y = (float) l.getY(), z = (float) l.getZ();
		float oX = (float) q.getX(), oY = (float) q.getY(), oZ = (float) q.getZ(), oW = (float) q.getW();
		gen.writeLine(id, crc, container, event, terrain.name(), x, y, z, oX, oY, oZ, oW, cellIndex, objectTagMap.getOrDefault(originalId, ""));
	}
	
	private void isValidBuildoutAreas(List<SWGObject> objects) {
		for (SWGObject obj : objects) {
			assert fallbackAreas.stream().mapToInt(area -> area.id).anyMatch(id -> obj.getBuildoutAreaId() == id) || areas.stream().mapToInt(area -> area.id).anyMatch(id -> obj.getBuildoutAreaId() == id) : "no area found: " + obj.getBuildoutAreaId();
		}
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
			this.index = area != null ? area.getIndex() : -1;
			this.x1 = (int) x1;
			this.z1 = (int) z1;
			this.x2 = (int) x2;
			this.z2 = (int) z2;
			this.id = id;
			this.adjust = adjust;
		}
		
		@Override
		public int compareTo(@NotNull GenBuildoutArea area) {
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
			if (!(o instanceof GenBuildoutArea))
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
