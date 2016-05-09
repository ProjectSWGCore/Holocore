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
import resources.PvpFlag;
import resources.Terrain;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.building.BuildingObject;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureDifficulty;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.OptionFlag;
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;
import resources.server_info.RelationalServerFactory;
import resources.spawn.SpawnerType;
import resources.spawn.Spawner;
import services.objects.ObjectManager;

public final class SpawnerService extends Service {
	
	private static final String GET_ALL_SPAWNERS_SQL = "SELECT static.x, static.y, static.z, static.heading, " // static columns
			+ "static.spawner_type, static.cell_id, static.active, static.mood, " // more static columns
			+ "buildings.object_id AS building_id, buildings.terrain_name AS building_terrain, " // building columns
			+ "creatures.iff_template AS iff, creatures.creature_name creatures.combat_level creatures.difficulty " // creature columns
			+ "npc_stats.HP, npc_stats.Action "	// npc_stats columns
			+ "FROM static, buildings, creatures, npc_stats "
			+ "WHERE buildings.building_id = static.building_id AND static.creature_id = creatures.creature_id AND creatures.combat_level = npc_stats.Level";
	private static final String IDLE_MOOD = "idle";
	
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
		long start = System.nanoTime();
		int count = 0;
		System.out.println("SpawnerService: Loading NPCs...");
		Log.i(this, "Loading NPCs...");
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("spawn/static.db", "static", "building/buildings", "creatures/creatures")) {
			try (ResultSet set = spawnerDatabase.executeQuery(GET_ALL_SPAWNERS_SQL)) {
				Location loc = new Location();
				while (set.next()) {
					if (set.getBoolean("active")) {
						loadSpawner(set, loc, spawnEggs);
						count++;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		double time = (System.nanoTime()-start)/1E6;
		System.out.printf("SpawnerService: Finished loading %d NPCs. Time: %fms%n", count, time);
		Log.i(this, "Finished loading %d NPCs. Time: %fms", count, time);
	}
	
	private void loadSpawner(ResultSet set, Location loc, boolean spawnEggs) throws SQLException {
		loc.setTerrain(Terrain.valueOf(set.getString("building_terrain")));
		loc.setPosition(set.getFloat("x"), set.getFloat("y"), set.getFloat("z"));
		loc.setHeading(set.getFloat("heading"));
		int cellId = set.getInt("cell_id");
		
		SWGObject parent = null;
		if (cellId > 0) {
			parent = objectManager.getObjectById(set.getLong("building_id"));
			if (parent instanceof BuildingObject)
				parent = ((BuildingObject) parent).getCellByNumber(cellId);
		}
		
		if (spawnEggs) {
			SpawnerType spawnerType = SpawnerType.valueOf(set.getString("spawner_type"));
			SWGObject egg = objectManager.createObject(parent, spawnerType.getObjectTemplate(), loc, false);
			spawners.add(new Spawner(egg));
		}
		String difficultyChar = set.getString("difficulty");
		String creatureName = set.getString("creature_name");
		CreatureDifficulty difficulty;
		
		switch(difficultyChar) {
			default: Log.w(this, "An unknown creature difficulty of %s was set for %s. Using default NORMAL", difficultyChar, creatureName);
			case "N": difficulty = CreatureDifficulty.NORMAL; break;
			case "E": difficulty = CreatureDifficulty.ELITE; break;
			case "B": difficulty = CreatureDifficulty.BOSS; break;
		}
		
		createNPC(parent, loc, set.getString("iff"), creatureName, set.getString("mood"), set.getInt("combat_level"), difficulty, set.getInt("HP"), set.getInt("Action"), set.getString("attackable"));
	}
	
	private boolean createNPC(SWGObject parent, Location loc, String iff, String name, String moodAnimation, int combatLevel, CreatureDifficulty difficulty, int health, int action, String attackable) {
		CreatureObject object = (CreatureObject) objectManager.createObject(parent, createTemplate(getRandomIff(iff)), loc, false);
		if (object == null)
			return false;
		object.setName(getCreatureName(name));
		object.setLevel((short) combatLevel);
		object.setDifficulty(difficulty);
		object.setHealth(health);
		object.setAction(action);
		
		switch(attackable) {
			case "AGGRESSIVE": object.setOptionFlags(OptionFlag.AGGRESSIVE);	// Ziggy: There's also an AGGRESSIVE PvpFlag?
			case "ATTACKABLE": object.setPvpFlags(PvpFlag.ATTACKABLE); break;
			case "INVULNERABLE": object.setOptionFlags(OptionFlag.INVULNERABLE); break;
			default: Log.w(this, "An unknown attackable type of %s was specified for %s", attackable, name); break;
		}
		
		if (!moodAnimation.equals(IDLE_MOOD)) {
			object.setMoodAnimation(moodAnimation);
		}
		return true;
	}
	
	private String getCreatureName(String name) {
		return name.replace("(", "\n(");
	}
	
	private String getRandomIff(String semicolonSeparated) {
		String [] possible = semicolonSeparated.split(";");
		return possible[(int) (Math.random()*possible.length)];
	}
	
	private String createTemplate(String template) {
		if (template.indexOf('/') != -1) {
			int ind = template.lastIndexOf('/');
			return "object/mobile/" + template.substring(0, ind) + "/shared_" + template.substring(ind+1);
		} else
			return "object/mobile/shared_" + template;
	}
	
	private void removeSpawners() {
		for(Spawner spawner : spawners)
			objectManager.destroyObject(spawner.getSpawnerObject());
		
		spawners.clear();
	}
}
