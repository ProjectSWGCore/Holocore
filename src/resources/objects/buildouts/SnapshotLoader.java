package resources.objects.buildouts;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
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
	
	public static List <SWGObject> loadAllSnapshots() {
		List <SWGObject> objects = new LinkedList<>();
		for (Terrain t : TERRAINS) {
			List <SWGObject> terrain = loadSnapshotsForTerrain(t);
			objects.addAll(terrain);
		}
		return objects;
	}
	
	public static List <SWGObject> loadSnapshotsForTerrain(Terrain t) {
		TerrainSnapshotLoader loader = new TerrainSnapshotLoader(t);
		loader.load();
		return loader.getObjects();
	}
	
}
