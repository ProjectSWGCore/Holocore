package resources.objects.buildouts;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import resources.Location;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.WorldSnapshotData;
import resources.client_info.visitors.WorldSnapshotData.Chunk;
import resources.containers.ContainerPermissions;
import resources.objects.SWGObject;
import resources.objects.cell.CellObject;
import resources.server_info.Log;
import services.objects.ObjectCreator;

public class TerrainSnapshotLoader {
	
	private static final String BASE_PATH = "snapshot/";
	
	private final Terrain terrain;
	private final Map <Long, SWGObject> objectTable;
	private final List <SWGObject> objects;
	
	public TerrainSnapshotLoader(Terrain terrain) {
		this.terrain = terrain;
		this.objectTable = new Hashtable<Long, SWGObject>(12*1024);
		this.objects = new LinkedList<>();
	}
	
	public List <SWGObject> getObjects() {
		return objects;
	}
	
	public void load() {
		objects.clear();
		String path = BASE_PATH + terrain.getName() + ".ws";
		WorldSnapshotData data = (WorldSnapshotData) ClientFactory.getInfoFromFile(path);
		Map <Integer, String> templates = data.getObjectTemplateNames();
		for (Chunk chunk : data.getChunks()) {
			SWGObject object = createObject(templates, chunk);
			object.setBuildout(true);
			object.setLoadRange(chunk.getRadius());
			addObject(object, chunk.getContainerId());
			setCellInformation(object, chunk.getCellIndex());
			updatePermissions(object);
		}
	}
	
	private SWGObject createObject(Map <Integer, String> templateMap, Chunk row) {
		SWGObject object = ObjectCreator.createObjectFromTemplate(row.getId(), templateMap.get(row.getObjectTemplateNameIndex()));
		Location l = row.getLocation();
		l.setTerrain(terrain);
		object.setLocation(l);
		return object;
	}
	
	private void addObject(SWGObject object, long containerId) {
		objectTable.put(object.getObjectId(), object);
		if (containerId != 0) {
			SWGObject container = objectTable.get(containerId);
			if (container != null)
				container.addObject(object);
			else {
				Log.e("TerrainSnapshotLoader", "Failed to load object: " + object.getTemplate());
			}
		} else {
			objects.add(object);
		}
	}
	
	private void setCellInformation(SWGObject object, int cellIndex) {
		if (!(object instanceof CellObject))
			return;
		CellObject cell = (CellObject) object;
		cell.setNumber(cellIndex);
	}
	
	private void updatePermissions(SWGObject object) {
		object.setContainerPermissions(ContainerPermissions.WORLD);
	}
	
}
