package services.spawn;

import java.util.ArrayList;
import java.util.Collection;

import intents.server.ConfigChangedIntent;
import resources.Location;
import resources.Terrain;
import resources.client_info.ServerFactory;
import resources.client_info.visitors.DatatableData;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.building.BuildingObject;
import resources.objects.SWGObject;
import resources.spawn.SpawnType;
import resources.spawn.Spawner;
import services.objects.ObjectManager;

public final class SpawnerService extends Service {

	private final ObjectManager objectManager;
	private final Collection<Spawner> spawners;
	
	public SpawnerService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		spawners = new ArrayList<>();
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(ConfigChangedIntent.TYPE);
		
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		ConfigChangedIntent cgi = (ConfigChangedIntent) i;
		String newValue, oldValue;
		
		if(cgi.getChangedConfig().equals(ConfigFile.FEATURES))
			if(cgi.getKey().equals("SPAWNERS-ENABLED")) {
				newValue = cgi.getNewValue();
				oldValue = cgi.getOldValue();
				
				if(!newValue.equals(oldValue)) {
					if(Boolean.valueOf(newValue) && spawners.isEmpty()) { // If nothing's been spawned, create it.
						loadEggs();
					} else { // If anything's been spawned, delete it.
						for(Spawner spawner : spawners)
							objectManager.destroyObject(spawner.getSpawnerObject());
						
						spawners.clear();
					}
				}
			}
		
	}
	
	public void loadEggs() {
		DatatableData eggs = ServerFactory.getDatatable("spawn/static.iff");
				
		eggs.handleRows(rowIndex -> {
			final Location loc;
			final int buildingId;
			final SWGObject egg;
			final SpawnType spawnType;
			final boolean spawnInCell;
			
			if((Integer) eggs.getCell(rowIndex, 12) == 1) { // We only spawn active eggs
				buildingId = (int) eggs.getCell(rowIndex, 3);
				loc = new Location((float) eggs.getCell(rowIndex, 5), (float) eggs.getCell(rowIndex, 6), (float) eggs.getCell(rowIndex, 7), Terrain.valueOf((String) eggs.getCell(rowIndex, 1)));
				spawnType = SpawnType.valueOf((String) eggs.getCell(rowIndex, 2));
				spawnInCell = buildingId != -1;
				egg = objectManager.createObject(spawnType.getObjectTemplate(), loc, !spawnInCell, false);
				
				if(spawnInCell) // If it wants to be inside a building, make it so!
					((BuildingObject) objectManager.getObjectById(buildingId)).getCellByName((String) eggs.getCell(rowIndex, 4)).addObject(egg);
			
				spawners.add(new Spawner(egg));
			}
		});
	}
}
