package resources.objects.buildouts;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	private final Map <Long, List <CellObject>> buildingCells;
	private final List <SWGObject> objects;
	private final List <Orphan> parentless;
	private final Location areaLocation;
	
	public TerrainBuildoutLoader(ClientFactory clientFactory, CrcStringTableData crcTable, Terrain terrain) {
		this.clientFactory = clientFactory;
		this.crcTable = crcTable;
		this.terrain = terrain;
		this.objectTable = new Hashtable<Long, SWGObject>(12*1024);
		this.buildingCells = new HashMap<Long, List <CellObject>>();
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
		objectTable.clear();
		String file = BASE_PATH+"areas_"+terrain.getName()+".iff";
		DatatableData areaTable = (DatatableData) clientFactory.getInfoFromFile(file);
		for (int row = 0; row < areaTable.getRowCount(); row++) {
			String areaName = (String) areaTable.getCell(row, 0);
			if (!areaName.startsWith(terrain.getName()))
				continue;
			areaLocation.setX((Float) areaTable.getCell(row, 1));
			areaLocation.setZ((Float) areaTable.getCell(row, 2));
			loadArea(areaName);
		}
		updateParentless();
		finalizeCells();
		objectTable.clear();
	}
	
	private void finalizeCells() {
		for (Entry <Long, List <CellObject>> entry : buildingCells.entrySet()) {
			SWGObject building = objectTable.get(entry.getKey());
			if (building == null)
				continue;
			List <CellObject> cells = entry.getValue();
			for (CellObject cell : cells) {
				building.addChild(cell);
			}
			cells.clear();
		}
		buildingCells.clear();
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
			Object [] columns = area.getRow(row);
			long objectId = (Integer) columns[0];
			String template = crcTable.getTemplateString((Integer) columns[3]);
			int container = (Integer) columns[1];
			loadRow(columns, objectId, template, container, 4);
		}
	}
	
	private void loadColumnsSmall(DatatableData area) {
		int rows = area.getRowCount();
		for (int row = 0; row < rows; row++) {
			Object [] columns = area.getRow(row);
			int crc = (Integer) columns[0];
			long objectId = ((long) crc) << 32 + 0xFFFF86F9L;
			String template = crcTable.getTemplateString(crc);
			loadRow(columns, objectId, template, 0, 1);
		}
	}
	
	private void loadRow(Object [] columns, long objectId, String template, long container, int cellOffset) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(objectId, template);
		if (obj == null) {
			System.err.println("Could not load object with template " + template);
			return;
		}
		obj.setLocation(readLocation(columns, cellOffset+1));
		objectTable.put(obj.getObjectId(), obj);
		if (obj instanceof CellObject) {
			((CellObject) obj).setNumber((Integer) columns[cellOffset]);
			loadCell((CellObject) obj, container);
		} else
			matchParent(obj, container);
	}
	
	private void loadCell(CellObject cell, long container) {
		List <CellObject> cells = buildingCells.get(container);
		if (cells == null) {
			cells = new LinkedList<CellObject>();
			buildingCells.put(container, cells);
			cells.add(cell);
			return;
		}
		int index = 0;
		Iterator <CellObject> it = cells.iterator();
		while (it.hasNext() && it.next().getNumber() < cell.getNumber()) {
			index++;
		}
		cells.add(index, cell);
	}
	
	private void matchParent(SWGObject obj, long containerId) {
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
	
	private Location readLocation(Object [] columns, int start) {
		Location l = new Location((Float) columns[start], (Float) columns[start+1], (Float) columns[start+2], terrain);
		l.setOrientationW((Float) columns[start+3]);
		l.setOrientationX((Float) columns[start+4]);
		l.setOrientationY((Float) columns[start+5]);
		l.setOrientationZ((Float) columns[start+6]);
		return l;
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
