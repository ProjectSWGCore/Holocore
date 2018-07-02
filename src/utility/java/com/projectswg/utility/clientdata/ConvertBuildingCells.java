package com.projectswg.utility.clientdata;

import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.data.swgfile.visitors.PortalLayoutData;
import com.projectswg.common.data.swgfile.visitors.PortalLayoutData.Cell;
import com.projectswg.utility.SdbGenerator;

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
		List<Point3D> doorPoints = new ArrayList<>(doorframes.size()/4);
		for (int i = 0; i+3 < doorframes.size(); i+=4) {
			double x = 0;
			double y = Double.MAX_VALUE;
			double z = 0;
			for (int j = i; j < i+4; j++) {
				Point3D p = doorframes.get(j);
				x += p.getX();
				if (p.getY() < y)
					y = p.getY();
				z += p.getZ();
			}
			x /= 4;
			z /= 4;
			doorPoints.add(new Point3D(x, y, z));
		}
		List<Cell> cells = portalLayoutData.getCells();
		Map<Point3D, List<Cell>> doors = new HashMap<>();
		for (Cell cell : cells) {
			for (int portal : cell.getPortals()) {
				doors.computeIfAbsent(doorPoints.get(portal), k -> new ArrayList<>()).add(cell);
			}
		}
		assert doors.values().stream().mapToInt(Collection::size).sum() == doors.size()*2;
		
		String building = file.getAbsolutePath().replace(CLIENTDATA.getAbsolutePath() + '/', "");
		for (Cell cell : cells) {
			StringBuilder neighbors = new StringBuilder();
			boolean first = true;
			for (int portal : cell.getPortals()) {
				Point3D portalLocation = doorPoints.get(portal);
				assert doors.get(portalLocation).size() == 2;
				for (Cell neighbor : doors.get(portalLocation)) {
					if (neighbor == cell)
						continue;
					if (!first)
						neighbors.append(';');
					first = false;
					neighbors.append(String.format("%d,%.2f,%.2f,%.2f",cells.indexOf(neighbor), portalLocation.getX(), portalLocation.getY(), portalLocation.getZ()));
				}
			}
			sdb.writeLine(building, cells.indexOf(cell), cell.getName(), neighbors);
		}
	}
	
}
