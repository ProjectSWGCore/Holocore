package resources.objects.buildouts;

import java.util.LinkedList;
import java.util.List;

import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.CrcStringTableData;
import resources.client_info.visitors.DatatableData;
import resources.objects.SWGObject;

public class BuildoutLoader {
	
	private static final ClientFactory clientFactory = new ClientFactory();
	private static final CrcStringTableData crcTable = (CrcStringTableData) clientFactory.getInfoFromFile("misc/object_template_crc_string_table.iff");
	
	public static List <SWGObject> loadAllBuildouts() {
		List <SWGObject> objects = new LinkedList<SWGObject>();
		for (Terrain t : getTerrainsToLoad())
			objects.addAll(loadBuildoutsForTerrain(t));
		return objects;
	}
	
	public static List <SWGObject> loadBuildoutsForTerrain(Terrain terrain) {
		TerrainBuildoutLoader loader = new TerrainBuildoutLoader(clientFactory, crcTable, terrain);
		loader.load();
		return loader.getObjects();
	}
	
	private static List <Terrain> getTerrainsToLoad() {
		DatatableData table = (DatatableData) clientFactory.getInfoFromFile("datatables/buildout/buildout_scenes.iff");
		List <Terrain> terrains = new LinkedList<Terrain>();
		for (int row = 0; row < table.getRowCount(); row++) {
			Terrain t = Terrain.getTerrainFromName((String) table.getCell(row, 0));
			if (t != null)
				terrains.add(t);
			else
				System.err.println("Couldn't find terrain: " + table.getCell(row, 0));
		}
		return terrains;
	}
	
}
