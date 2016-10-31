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

import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.server.ConfigChangedIntent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import resources.Location;
import resources.PvpFlag;
import resources.Terrain;
import resources.config.ConfigFile;
import resources.containers.ContainerPermissionsType;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.building.BuildingObject;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureDifficulty;
import resources.objects.creature.CreatureObject;
import resources.objects.custom.AIBehavior;
import resources.objects.custom.DefaultAIObject;
import resources.objects.tangible.OptionFlag;
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;
import resources.server_info.RelationalServerFactory;
import resources.spawn.SpawnerType;
import resources.spawn.Spawner;
import services.objects.ObjectCreator;
import services.objects.ObjectManager;
import utilities.ThreadUtilities;

public final class SpawnerService extends Service {
	
	private static final String GET_ALL_SPAWNERS_SQL = "SELECT static.x, static.y, static.z, static.heading, " // static columns
			+ "static.spawner_type, static.cell_id, static.active, static.mood, static.behaviour, static.float_radius, " // more static columns
			+ "static.min_spawn_time, static.max_spawn_time, " // even more static columns
			+ "buildings.object_id AS building_id, buildings.terrain_name AS building_terrain, " // building columns
			+ "creatures.iff_template AS iff, creatures.creature_name, creatures.combat_level, creatures.difficulty, creatures.attackable, " // creature columns
			+ "npc_stats.HP, npc_stats.Action, npc_stats.Boss_HP, npc_stats.Boss_Action, npc_stats.Elite_HP, npc_stats.Elite_Action "	// npc_stats columns
			+ "FROM static, buildings, creatures, npc_stats "
			+ "WHERE buildings.building_id = static.building_id AND static.creature_id = creatures.creature_id AND creatures.combat_level = npc_stats.Level";
	private static final String IDLE_MOOD = "idle";
	
	private final ObjectManager objectManager;
	private final Map<DefaultAIObject, Spawner> spawnerMap;
	private final ScheduledExecutorService executorService;
	
	public SpawnerService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		executorService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("spawner-service"));
		spawnerMap = new HashMap<>();
		
		registerForIntent(ConfigChangedIntent.TYPE);
		registerForIntent(DestroyObjectIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		if(getConfig(ConfigFile.FEATURES).getBoolean("SPAWN-EGGS-ENABLED", true))
			loadSpawners();
		
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case ConfigChangedIntent.TYPE: handleConfigChangedIntent((ConfigChangedIntent) i); break;
			case DestroyObjectIntent.TYPE: handleDestroyObjectIntent((DestroyObjectIntent) i); break;
		}
	}

	@Override
	public boolean terminate() {
		executorService.shutdown();
		
		return super.terminate();
	}
	
	private void handleConfigChangedIntent(ConfigChangedIntent i) {
		String newValue, oldValue;
		
		if (i.getChangedConfig().equals(ConfigFile.FEATURES) && i.getKey().equals("SPAWN-EGGS-ENABLED")) {
			newValue = i.getNewValue();
			oldValue = i.getOldValue();

			if (!newValue.equals(oldValue)) {
				if (Boolean.valueOf(newValue) && spawnerMap.isEmpty()) { // If nothing's been spawned, create it.
					loadSpawners();
				} else { // If anything's been spawned, delete it.
					removeSpawners();
				}
			}
		}
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent i) {
		SWGObject destroyedObject = i.getObject();
		
		if(destroyedObject instanceof DefaultAIObject) {
			DefaultAIObject killedAIObject = (DefaultAIObject) destroyedObject;
			
			Spawner spawner = spawnerMap.remove(killedAIObject);
			
			if(spawner == null) {
				Log.e(this, "Killed AI object %s has no linked Spawner - it cannot respawn!", killedAIObject);
				return;
			}
			
			executorService.schedule(() -> spawnNPC(spawner), spawner.getRespawnDelay(), TimeUnit.SECONDS);
		}
	}
	
	private void loadSpawners() {
		long start = System.nanoTime();
		int count = 0;
		Log.i(this, "Loading NPCs...");
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("spawn/static.db", "static", "building/buildings", "creatures/creatures", "creatures/npc_stats")) {
			try (ResultSet set = spawnerDatabase.executeQuery(GET_ALL_SPAWNERS_SQL)) {
				Location loc = new Location();
				while (set.next()) {
					if (set.getBoolean("active")) {
						loadSpawner(set, loc);
						count++;
					}
				}
			} catch (SQLException e) {
				Log.e(this, e);
			}
		}
		double time = (System.nanoTime()-start)/1E6;
		Log.i(this, "Finished loading %d NPCs. Time: %fms", count, time);
	}
	
	private void loadSpawner(ResultSet set, Location loc) throws SQLException {
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
		
		String difficultyChar = set.getString("difficulty");
		String creatureName = set.getString("creature_name");
		CreatureDifficulty difficulty;
		int maxHealth = 0;
		int maxAction = 0;
		
		switch(difficultyChar) {
			default: Log.w(this, "An unknown creature difficulty of %s was set for %s. Using default NORMAL", difficultyChar, creatureName);
			case "N":
				difficulty = CreatureDifficulty.NORMAL;
				maxHealth = set.getInt("HP");
				maxAction = set.getInt("Action");
				break;
			case "E":
				difficulty = CreatureDifficulty.ELITE;
				maxHealth = set.getInt("Elite_HP");
				maxAction = set.getInt("Elite_Action");
				break;
			case "B":
				difficulty = CreatureDifficulty.BOSS;
				maxHealth = set.getInt("Boss_HP");
				maxAction = set.getInt("Boss_Action");
				break;
		}
		
		SpawnerType spawnerType = SpawnerType.valueOf(set.getString("spawner_type"));
		SWGObject egg = ObjectCreator.createObjectFromTemplate(spawnerType.getObjectTemplate());
		egg.setContainerPermissions(ContainerPermissionsType.ADMIN);
		egg.setLocation(loc);
		egg.moveToContainer(parent);
		new ObjectCreatedIntent(egg).broadcast();

		Spawner spawner = new Spawner(egg);
		spawner.setIffTemplates(set.getString("iff").split(";"));
		spawner.setCreatureName(set.getString("creature_name"));
		spawner.setMinRespawnDelay(set.getInt("min_spawn_time"));
		spawner.setMaxRespawnDelay(set.getInt("max_spawn_time"));
		spawner.setMaxHealth(maxHealth);
		spawner.setMaxAction(maxAction);
		spawner.setCreatureDifficulty(difficulty);
		spawner.setCombatLevel((short) set.getInt("combat_level"));
		String moodAnimation = set.getString("mood");
		spawner.setFlagString(set.getString("attackable"));

		AIBehavior aiBehavior = AIBehavior.valueOf(set.getString("behaviour"));
		spawner.setAIBehavior(aiBehavior);

		if (!moodAnimation.equals(IDLE_MOOD)) {
			spawner.setMoodAnimation(moodAnimation);
		}

		if (aiBehavior == AIBehavior.FLOAT) {
			spawner.setFloatRadius((Integer) set.getInt("float_radius"));
		}

		spawnNPC(spawner);
	}
	
	private void spawnNPC(Spawner spawner) {
		spawnerMap.put(createNPC(spawner), spawner);
	}
	
	private DefaultAIObject createNPC(Spawner spawner) {
		DefaultAIObject object = ObjectCreator.createObjectFromTemplate(createTemplate(spawner.getRandomIffTemplate()), DefaultAIObject.class);
		SWGObject spawnerObject = spawner.getSpawnerObject();
		SWGObject spawnerObjectParent = spawnerObject.getParent();
		object.setLocation(spawnerObject.getLocation());
		
		if (spawnerObjectParent != null)
			object.moveToContainer(spawnerObjectParent);
		
		object.setName(spawner.getCreatureName());
		object.setLevel(spawner.getCombatLevel());
		object.setDifficulty(spawner.getCreatureDifficulty());
		object.setMaxHealth(spawner.getMaxHealth());
		object.setHealth(spawner.getMaxHealth());
		object.setMaxAction(spawner.getMaxAction());
		object.setAction(spawner.getMaxAction());
		setFlags(object, spawner.getFlagString());
		
		object.setBehavior(spawner.getAIBehavior());
		if (object.getBehavior() == AIBehavior.FLOAT)
			object.setFloatRadius(spawner.getFloatRadius());
		
		String moodAnimation = spawner.getMoodAnimation();
		if (moodAnimation != null) {
			object.setMoodAnimation(moodAnimation);
		}
		new ObjectCreatedIntent(object).broadcast();
		return object;
	}
	
	private void setFlags(CreatureObject creature, String flagString) {
		switch (flagString) {
			case "AGGRESSIVE":
				creature.setPvpFlags(PvpFlag.AGGRESSIVE);
				creature.addOptionFlags(OptionFlag.AGGRESSIVE);
			case "ATTACKABLE":
				creature.setPvpFlags(PvpFlag.ATTACKABLE);
				creature.addOptionFlags(OptionFlag.HAM_BAR);
				break;
			case "INVULNERABLE":
				creature.addOptionFlags(OptionFlag.INVULNERABLE);
				break;
			default:
				Log.w(this, "An unknown attackable type of %s was specified for %s", flagString, creature.getName());
				break;
		}
	}
	
	private String createTemplate(String template) {
		if (template.indexOf('/') != -1) {
			int ind = template.lastIndexOf('/');
			return "object/mobile/" + template.substring(0, ind) + "/shared_" + template.substring(ind+1);
		} else
			return "object/mobile/shared_" + template;
	}
	
	private void removeSpawners() {
		spawnerMap.values().forEach(spawner -> new DestroyObjectIntent(spawner.getSpawnerObject()).broadcast());
		spawnerMap.clear();
	}
}
