package com.projectswg.utility.clientdata;

import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.data.swgfile.visitors.PortalLayoutData;
import com.projectswg.common.data.swgfile.visitors.PortalLayoutData.Cell;
import com.projectswg.holocore.utilities.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

class ConvertBuildingCells implements Converter {
	
	private static final File CLIENTDATA = new File("clientdata");
	private static final File BUILDINGS = new File(CLIENTDATA, "object/building");
	
	public ConvertBuildingCells() {
		
	}
	
	@Override
	public void convert() {
		System.out.println("Converting building cells...");
		try (SdbGenerator sdb = new SdbGenerator(new File("serverdata/objects/building_cells.sdb"))) {
			sdb.writeColumnNames("building", "index", "name", "neighbors");
			Converter.traverseFiles(this, BUILDINGS, sdb, file -> file.getName().endsWith(".iff"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void convertFile(SdbGenerator sdb, File file) throws IOException {
		ObjectData objectData = (ObjectData) ClientFactory.getInfoFromFile(file);
		if (objectData == null) {
			System.err.println("Failed to load object: " + file);
			return;
		}
		String portalFile = (String) objectData.getAttribute(ObjectDataAttribute.PORTAL_LAYOUT_FILENAME);
		if (portalFile == null || portalFile.isEmpty()) {
			return; // Many buildings don't have portals (any building you can't enter)
		}
		
		PortalLayoutData portalLayoutData = (PortalLayoutData) ClientFactory.getInfoFromFile(portalFile);
		if (portalLayoutData == null) {
			System.err.println("Failed to load object (invalid portal): " + file);
			return;
		}
		
		List<Point3D> doorframes = portalLayoutData.getRadarPoints();
		List<Portal> doorPoints = new ArrayList<>(doorframes.size()/4);
		for (int i = 0; i+3 < doorframes.size(); i+=4) {
			List<Point3D> door = new ArrayList<>();
			for (int j = i; j < i+4; j++) {
				door.add(doorframes.get(j));
			}
			door.sort(Comparator.comparingDouble(Point3D::getY));
			doorPoints.add(new Portal(door.get(0), door.get(1), door.get(2).getY() - door.get(0).getY()));
		}
		List<Cell> cells = portalLayoutData.getCells();
		Map<Portal, List<Cell>> doors = new HashMap<>();
		for (Cell cell : cells) {
			for (int portal : cell.getPortals()) {
				doors.computeIfAbsent(doorPoints.get(portal), k -> new ArrayList<>()).add(cell);
			}
		}
		assert doors.values().stream().mapToInt(Collection::size).sum() == doors.size()*2;
		
		String building = file.getAbsolutePath().replace(CLIENTDATA.getAbsolutePath() + '/', "");
		for (Cell cell : cells) {
			StringBuilder neighbors = new StringBuilder("[");
			boolean first = true;
			for (int portal : cell.getPortals()) {
				Portal portalLocation = doorPoints.get(portal);
				assert doors.get(portalLocation).size() == 2;
				for (Cell neighbor : doors.get(portalLocation)) {
					if (neighbor == cell)
						continue;
					if (!first)
						neighbors.append(',');
					first = false;
					neighbors.append(String.format("[%d,%.2f,%s,%s]", cells.indexOf(neighbor), portalLocation.getHeight(), toString(portalLocation.getFrame1()), toString(portalLocation.getFrame2())));
				}
			}
			sdb.writeLine(building, cells.indexOf(cell), cell.getName(), neighbors.append(']').toString());
		}
	}
	
	private static String toString(Point3D point) {
		return String.format("[%.2f,%.2f,%.2f]", point.getX(), point.getY(), point.getZ());
	}
	
	private static class Portal {
		
		private final Point3D frame1;
		private final Point3D frame2;
		private final double height;
		
		public Portal(Point3D frame1, Point3D frame2, double height) {
			this.frame1 = frame1;
			this.frame2 = frame2;
			this.height = height;
		}
		
		public Point3D getFrame1() {
			return frame1;
		}
		
		public Point3D getFrame2() {
			return frame2;
		}
		
		public double getHeight() {
			return height;
		}
		
		@Override
		public String toString() {
			return String.format("[[%.2f,%.2f,%.2f],[%.2f,%.2f,%.2f],%.2f]", frame1.getX(), frame1.getY(), frame1.getZ(), frame2.getX(), frame2.getY(), frame2.getZ(), height);
		}
	}
	
}
