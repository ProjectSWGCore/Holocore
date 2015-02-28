package resources.objects.buildouts;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import resources.Location;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.CrcStringTableData;
import resources.client_info.visitors.DatatableData;
import resources.objects.SWGObject;
import resources.objects.cell.CellObject;
import services.objects.ObjectCreator;

class TerrainBuildoutLoader {
	
	private static final String BASE_PATH = "datatables/buildout/";
	
	private final ClientFactory clientFactory;
	private final CrcStringTableData crcTable;
	private final Terrain terrain;
	private final Hashtable <Long, SWGObject> objectTable;
	private final List <SWGObject> objects;
	private final List <Orphan> parentless;
	private final Location areaLocation;
	
	public TerrainBuildoutLoader(ClientFactory clientFactory, CrcStringTableData crcTable, Terrain terrain) {
		this.clientFactory = clientFactory;
		this.crcTable = crcTable;
		this.terrain = terrain;
		this.objectTable = new Hashtable<Long, SWGObject>(12*1024);
		this.objects = new LinkedList<SWGObject>();
		this.parentless = new LinkedList<Orphan>();
		this.areaLocation = new Location(0, 0, 0, terrain);
	}
	
	public void load() {
		objects.clear();
		loadAreas();
	}
	
	public List <SWGObject> getObjects() {
		return objects;
	}
	
	private void loadAreas() {
		String file = BASE_PATH+"areas_"+terrain.getName()+".iff";
		DatatableData areaTable = (DatatableData) clientFactory.getInfoFromFile(file);
		for (int row = 0; row < areaTable.getRowCount(); row++) {
			if (!((String) areaTable.getCell(row, 0)).startsWith(terrain.getName()))
				continue;
			areaLocation.setX((Float) areaTable.getCell(row, 1));
			areaLocation.setZ((Float) areaTable.getCell(row, 2));
			loadArea((String) areaTable.getCell(row, 0));
		}
		updateParentless();
	}
	
	private void loadArea(String areaName) {
		String file = BASE_PATH+terrain.getName()+"/"+areaName.replaceAll("server", "client")+".iff";
		DatatableData area = (DatatableData) clientFactory.getInfoFromFile(file);
		switch (area.getColumnCount()) {
			case 11:
				loadColumnsSmall(area);
				break;
			case 14:
				loadColumnsLarge(area);
				break;
			default:
				System.err.println("Unable to process iff with " + area.getColumnCount() + " columns. File: " + file);
				break;
		}
	}
	
	private void loadColumnsLarge(DatatableData area) {
		int rows = area.getRowCount();
		for (int row = 0; row < rows; row++) {
			long objectId = (Integer) area.getCell(row, 0);
			String template = crcTable.getTemplateString((Integer) area.getCell(row, 3));
			long container = ((Integer) area.getCell(row, 1)).longValue();
			loadRow(area, row, objectId, template, container, 4);
		}
	}
	
	private void loadColumnsSmall(DatatableData area) {
		int rows = area.getRowCount();
		for (int row = 0; row < rows; row++) {
			int crc = (Integer) area.getCell(row, 0);
			long objectId = ((long) crc) << 32 + 0xFFFF86F9L;
			String template = crcTable.getTemplateString(crc);
			loadRow(area, row, objectId, template, 0, 1);
		}
	}
	
	private void loadRow(DatatableData area, int row, long objectId, String template, long container, int cellOffset) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(objectId, template);
		if (obj == null) {
			System.err.println("Could not load object with template " + template);
			return;
		}
		obj.setLocation(readLocation(area, row, cellOffset+1));
		if (obj instanceof CellObject)
			((CellObject) obj).setNumber((Integer) area.getCell(row, cellOffset));
		matchParent(obj, container);
	}
	
	private void matchParent(SWGObject obj, long containerId) {
		objectTable.put(obj.getObjectId(), obj);
		if (containerId == 0) {
			obj.setLocation(obj.getLocation().translate(areaLocation));
			objects.add(obj);
			return;
		}
		SWGObject container = objectTable.get(containerId);
		if (container == null) {
			parentless.add(new Orphan(obj, containerId));
		} else {
			container.addChild(obj);
			objects.add(obj);
		}
	}
	
	private void updateParentless() {
		Iterator <Orphan> orphanIt = parentless.iterator();
		while (orphanIt.hasNext()) {
			Orphan orphan = orphanIt.next();
			SWGObject container = objectTable.get(orphan.getContainer());
			if (container != null) {
				container.addChild(orphan.getObject());
				orphanIt.remove();
			}
		}
	}
	
	private Location readLocation(DatatableData area, int row, int startCol) {
		Location l = new Location();
		l.setTerrain(terrain);
		l.setX(readFloat(area, row, startCol+0));
		l.setY(readFloat(area, row, startCol+1));
		l.setZ(readFloat(area, row, startCol+2));
		l.setOrientationW(readFloat(area, row, startCol+3));
		l.setOrientationX(readFloat(area, row, startCol+4));
		l.setOrientationY(readFloat(area, row, startCol+5));
		l.setOrientationZ(readFloat(area, row, startCol+6));
		return l;
	}
	
	private double readFloat(DatatableData area, int row, int col) {
		return (Float) area.getCell(row, col);
	}
	
	private class Orphan {
		private SWGObject obj;
		private long container;
		
		public Orphan(SWGObject obj, long container) {
			this.obj = obj;
			this.container = container;
		}
		
		public SWGObject getObject() { return obj; }
		public long getContainer() { return container; }
	}
	
}
