package resources.objects.buildouts;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import resources.Terrain;
import resources.objects.SWGObject;

public class SnapshotLoader {
	
	private static final Set <Terrain> TERRAINS = EnumSet.of(
			Terrain.CORELLIA,	Terrain.DANTOOINE,	Terrain.DATHOMIR,
			Terrain.DUNGEON1,	Terrain.ENDOR,		Terrain.LOK,
			Terrain.NABOO,		Terrain.RORI,		Terrain.TALUS,
			Terrain.TATOOINE,	Terrain.YAVIN4
	);
	
	private Map <Long, SWGObject> objectTable;
	private List <SWGObject> objects;
	
	public SnapshotLoader() {
		objectTable = new HashMap<>();
		objects = new LinkedList<>();
	}
	
	public void loadAllSnapshots() {
		for (Terrain t : TERRAINS) {
			loadSnapshotsForTerrain(t);
		}
	}
	
	public void loadSnapshotsForTerrain(Terrain t) {
		TerrainSnapshotLoader loader = new TerrainSnapshotLoader(t);
		loader.load();
		objects.addAll(loader.getObjects());
		objectTable.putAll(loader.getObjectTable());
	}
	
	public Map<Long, SWGObject> getObjectTable() {
		return objectTable;
	}
	
	public List <SWGObject> getObjects() {
		return objects;
	}
	
}
