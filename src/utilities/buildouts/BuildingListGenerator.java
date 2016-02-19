package utilities.buildouts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import resources.Terrain;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import services.objects.ClientBuildoutService;
import utilities.SdbGenerator;

public class BuildingListGenerator {
	
	public static void main(String [] args) throws IOException {
		ClientBuildoutService buildouts = new ClientBuildoutService();
		Collection<SWGObject> objects = buildouts.loadClientObjects();
		System.out.println("Organizing data...");
		List<BuildingObject> buildings = new ArrayList<>();
		SdbGenerator gen = new SdbGenerator(new File("buildings.sdb"));
		gen.open();
		initializeSdb(gen);
		for (SWGObject obj : objects) {
			if (obj instanceof BuildingObject) {
				buildings.add((BuildingObject) obj);
			}
		}
		buildings.sort((b1, b2) -> {
			int comp = b1.getTerrain().compareTo(b2.getTerrain());
			if (comp != 0)
				return comp;
			comp = Long.compareUnsigned(b1.getObjectId(), b2.getObjectId());
			return comp;
		});
		System.out.println("Writing buildings...");
		for (BuildingObject building : buildings) {
			String template = building.getTemplate().substring(building.getTemplate().lastIndexOf('/')+1);
			Terrain terrain = building.getTerrain();
			String name = terrain.name().charAt(0) + terrain.name().toLowerCase(Locale.US).substring(1);
			int cells = building.getCells().size();
			if (cells <= 0)
				continue;
			gen.writeLine("", terrain, building.getObjectId(), template, name, cells, building.getX(), building.getY(), building.getZ());
		}
		System.out.println("Finished.");
		gen.close();
		System.exit(0);
	}
	
	private static void initializeSdb(SdbGenerator gen) throws IOException {
		final String intType = "INTEGER NOT NULL";
		final String realType = "REAL NOT NULL";
		gen.setColumnNames("building_id", "terrain_name", "object_id", "iff", "building_name", "total_cells", "x", "y", "z");
		gen.setColumnTypes("TEXT PRIMARY KEY", "TEXT NOT NULL", intType, "TEXT NOT NULL", "TEXT NOT NULL", intType, realType, realType, realType);
	}
	
}
