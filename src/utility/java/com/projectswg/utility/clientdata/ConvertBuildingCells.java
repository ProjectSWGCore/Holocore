package com.projectswg.utility.clientdata;

import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.math.IndexedTriangleList;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.data.swgiff.parsers.SWGParser;
import com.projectswg.common.data.swgiff.parsers.appearance.PortalLayoutCellPortalTemplate;
import com.projectswg.common.data.swgiff.parsers.appearance.PortalLayoutCellTemplate;
import com.projectswg.common.data.swgiff.parsers.appearance.PortalLayoutTemplate;
import com.projectswg.holocore.utilities.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
		
		PortalLayoutTemplate portalLayoutData = SWGParser.parse(portalFile);
		if (portalLayoutData == null) {
			System.err.println("Failed to load object (invalid portal): " + file);
			return;
		}
		
		String building = file.getAbsolutePath().replace(CLIENTDATA.getAbsolutePath() + '/', "");
		List<PortalLayoutCellTemplate> cells = portalLayoutData.getCells();
		List<IndexedTriangleList> portals = portalLayoutData.getPortals();
		
		for (PortalLayoutCellTemplate cell : cells) {
			StringBuilder neighbors = new StringBuilder("[");
			boolean first = true;
			for (PortalLayoutCellPortalTemplate portal : cell.getPortals()) {
				IndexedTriangleList portalData = portals.get(portal.getGeometryIndex());
				
				PortalLayoutCellTemplate neighbor;
				{
					PortalLayoutCellTemplate neighborTest = null;
					for (PortalLayoutCellTemplate neighborCandidate : cells) {
						if (neighborCandidate == cell)
							continue;
						for (PortalLayoutCellPortalTemplate neighborPortalCandidate : neighborCandidate.getPortals()) {
							if (neighborPortalCandidate.getGeometryIndex() == portal.getGeometryIndex()) {
								neighborTest = neighborCandidate;
								break;
							}
						}
						if (neighborTest != null)
							break;
					}
					neighbor = neighborTest;
				}
				
				if (!first)
					neighbors.append(',');
				first = false;
				
				// minX,minY,minZ,maxX,maxY,maxZ
				neighbors.append(String.format("[%d,[%.2f,%.2f,%.2f],[%.2f,%.2f,%.2f]]", cells.indexOf(neighbor),
						portalData.getVertices().stream().mapToDouble(Point3D::getX).min().orElseThrow(),
						portalData.getVertices().stream().mapToDouble(Point3D::getY).min().orElseThrow(),
						portalData.getVertices().stream().mapToDouble(Point3D::getZ).min().orElseThrow(),
						portalData.getVertices().stream().mapToDouble(Point3D::getX).max().orElseThrow(),
						portalData.getVertices().stream().mapToDouble(Point3D::getY).max().orElseThrow(),
						portalData.getVertices().stream().mapToDouble(Point3D::getZ).max().orElseThrow()
				));
			}
			sdb.writeLine(building, cells.indexOf(cell), cell.getName(), neighbors.append(']').toString());
		}
	}
	
}
