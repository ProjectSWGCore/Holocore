/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
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
		
		if (getConfig(ConfigFile.FEATURES).getBoolean("SPAWNERS-ENABLED", true))
			loadEggs();
		
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
	
	private void loadEggs() {
		DatatableData eggs = ServerFactory.getDatatable("spawn/static.iff");
				
		eggs.handleRows(rowIndex -> {
			if((Integer) eggs.getCell(rowIndex, 12) == 1) { // We only spawn active eggs
				final int buildingId = (int) eggs.getCell(rowIndex, 3);
				final Location loc = new Location((float) eggs.getCell(rowIndex, 5), (float) eggs.getCell(rowIndex, 6), (float) eggs.getCell(rowIndex, 7), Terrain.valueOf((String) eggs.getCell(rowIndex, 1)));
				final SpawnType spawnType = SpawnType.valueOf((String) eggs.getCell(rowIndex, 2));
				final boolean spawnInCell = buildingId != -1;
				final SWGObject egg = objectManager.createObject(spawnType.getObjectTemplate(), loc, !spawnInCell, false);
				
				if(spawnInCell) // If it wants to be inside a building, make it so!
					((BuildingObject) objectManager.getObjectById(buildingId)).getCellByName((String) eggs.getCell(rowIndex, 4)).addObject(egg);
			
				spawners.add(new Spawner(egg));
			}
		});
	}
}
