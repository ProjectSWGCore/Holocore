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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
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
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;
import resources.spawn.SpawnerType;
import resources.spawn.Spawner;
import services.objects.ObjectManager;

public final class SpawnerService extends Service {
	
	private static final String GET_ALL_SPAWNERS_SQL = "SELECT static.*, buildings.object_id, buildings.terrain_name, creatures.iff_template "
			+ "FROM static, buildings, creatures "
			+ "WHERE buildings.building_id = static.building_id AND static.creature_id = creatures.creature_id";
	
	private final ObjectManager objectManager;
	private final Collection<Spawner> spawners;
	
	public SpawnerService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		spawners = new ArrayList<>();
		
		registerForIntent(ConfigChangedIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		if (getConfig(ConfigFile.FEATURES).getBoolean("NPCS-ENABLED", true))
			loadSpawners(getConfig(ConfigFile.FEATURES).getBoolean("SPAWN-EGGS-ENABLED", false));
		
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (!(i instanceof ConfigChangedIntent))
			return;
		ConfigChangedIntent cgi = (ConfigChangedIntent) i;
		String newValue, oldValue;
		
		if(cgi.getChangedConfig().equals(ConfigFile.FEATURES))
			if(cgi.getKey().equals("NPCS-ENABLED")) {
				newValue = cgi.getNewValue();
				oldValue = cgi.getOldValue();
				
				if(!newValue.equals(oldValue)) {
					if(Boolean.valueOf(newValue) && spawners.isEmpty()) { // If nothing's been spawned, create it.
						loadSpawners(getConfig(ConfigFile.FEATURES).getBoolean("SPAWN-EGGS-ENABLED", false));
					} else { // If anything's been spawned, delete it.
						removeSpawners();
					}
				}
			}
		
	}
	
	private void loadSpawners(boolean spawnEggs) {
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("spawn/static.db", "static", "building/buildings", "creatures/creatures")) {
			try (ResultSet jointTable = spawnerDatabase.executeQuery(GET_ALL_SPAWNERS_SQL)) {
				while (jointTable.next()) {
					if (jointTable.getBoolean("active")) {
						loadSpawner(jointTable, spawnEggs);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSpawner(ResultSet set, boolean spawnEggs) throws SQLException {
		Location loc = new Location(set.getFloat("x"), set.getFloat("y"), set.getFloat("z"), Terrain.valueOf(set.getString("terrain_name")));
		SpawnerType spawnerType = SpawnerType.valueOf(set.getString("spawner_type"));
		long objectId = set.getLong("object_id");
		int cellId = set.getInt("cell_id");
		loc.setOrientation(set.getFloat("oX"), set.getFloat("oY"), set.getFloat("oZ"), set.getFloat("oW"));
		
		SWGObject parent = null;
		if (cellId > 0) {
			parent = objectManager.getObjectById(objectId);
			if (parent instanceof BuildingObject)
				parent = ((BuildingObject) parent).getCellByNumber(cellId);
		}
		
		if (spawnEggs) {
			SWGObject egg = objectManager.createObject(parent, spawnerType.getObjectTemplate(), loc, false);
			spawners.add(new Spawner(egg));
		}
		createNPC(parent, loc, set.getString("iff_template"), set.getString("creature_name"));
	}
	
	private void createNPC(SWGObject parent, Location loc, String iff, String name) {
		String [] possible = iff.split(";");
		String template = possible[(int) (Math.random()*possible.length)];
		if (template.contains("/")) {
			int ind = template.lastIndexOf('/');
			template = "object/mobile/" + template.substring(0, ind) + "/shared_" + template.substring(ind+1);
		} else
			template = "object/mobile/shared_" + template;
		File f = new File("clientdata/"+template);
		if (f.isFile()) {
			SWGObject object = objectManager.createObject(parent, template, loc, false);
			object.setName(name);
		} else
			Log.e(this, "Unknown template: " + template);
	}
	
	private void removeSpawners() {
		for(Spawner spawner : spawners)
			objectManager.destroyObject(spawner.getSpawnerObject());
		
		spawners.clear();
	}
}