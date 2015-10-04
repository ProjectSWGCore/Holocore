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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import intents.server.ConfigChangedIntent;
import resources.Location;
import resources.Terrain;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.building.BuildingObject;
import resources.objects.SWGObject;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;
import resources.spawn.SpawnerType;
import resources.spawn.Spawner;
import services.objects.ObjectManager;

public final class SpawnerService extends Service {
	
	private static final String GET_ALL_SPAWNERS_SQL = "SELECT static.*, buildings.object_id, buildings.terrain_name FROM static, buildings WHERE buildings.building_id = static.building_id";
	
	private final ObjectManager objectManager;
	private final Collection<Spawner> spawners;
	private final RelationalServerData spawnerDatabase;
	
	public SpawnerService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		spawners = new ArrayList<>();
		spawnerDatabase = RelationalServerFactory.getServerData("spawn/static.db", "static", "building/buildings");
		if (spawnerDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for SpawnerService");
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(ConfigChangedIntent.TYPE);
		
		if (getConfig(ConfigFile.FEATURES).getBoolean("SPAWNERS-ENABLED", false))
			loadSpawners();
		
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
						loadSpawners();
					} else { // If anything's been spawned, delete it.
						removeSpawners();
					}
				}
			}
		
	}
	
	@Override
	public boolean terminate() {
		spawnerDatabase.close();
		return super.terminate();
	}
	
	private void loadSpawners() {
		try(ResultSet jointTable = spawnerDatabase.prepareStatement(GET_ALL_SPAWNERS_SQL).executeQuery()) {
			while (jointTable.next()) {
				if(jointTable.getBoolean("active")) {
					Location loc = new Location(jointTable.getFloat("x"), jointTable.getFloat("y"), jointTable.getFloat("z"), Terrain.valueOf(jointTable.getString("terrain_name")));
					SpawnerType spawnerType = SpawnerType.valueOf(jointTable.getString("spawner_type"));
					long objectId = jointTable.getLong("object_id");
					long cellId = jointTable.getLong("cell_id");
					final boolean spawnInCell = cellId > 0;
					loc.setOrientation(jointTable.getFloat("oX"), jointTable.getFloat("oY"), jointTable.getFloat("oZ"), jointTable.getFloat("oW"));
					
					final SWGObject egg = objectManager.createObject(spawnerType.getObjectTemplate(), loc, !spawnInCell, false);
					
					if(spawnInCell)
						((BuildingObject) objectManager.getObjectById(objectId)).getCellByNumber(jointTable.getInt("cell_id")).addObject(egg);
				
					spawners.add(new Spawner(egg));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void removeSpawners() {
		for(Spawner spawner : spawners)
			objectManager.destroyObject(spawner.getSpawnerObject());
		
		spawners.clear();
	}
}