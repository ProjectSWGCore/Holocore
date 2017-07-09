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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.info.RelationalDatabase;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.debug.Log;

import intents.FactionIntent;
import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.server.ConfigChangedIntent;
import resources.PvpFaction;
import resources.PvpFlag;
import resources.PvpStatus;
import resources.config.ConfigFile;
import resources.containers.ContainerPermissionsType;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.creature.CreatureDifficulty;
import resources.objects.creature.CreatureObject;
import resources.objects.custom.AIBehavior;
import resources.objects.custom.DefaultAIObject;
import resources.objects.tangible.OptionFlag;
import resources.objects.tangible.TangibleObject;
import resources.server_info.DataManager;
import resources.server_info.StandardLog;
import resources.spawn.Spawner;
import resources.spawn.Spawner.SpawnerFlag;
import resources.spawn.SpawnerType;
import services.objects.ObjectCreator;
import services.objects.ObjectManager;
import utilities.ThreadUtilities;

public final class SpawnerService extends Service {
	
	private static final String GET_ALL_SPAWNERS_SQL = "SELECT static.x, static.y, static.z, static.heading, " // static columns
			+ "static.spawner_type, static.cell_id, static.active, static.mood, static.behaviour, static.float_radius, " // more static columns
			+ "static.min_spawn_time, static.max_spawn_time, static.amount, static.spawn_id, " // even more static columns
			+ "buildings.object_id AS building_id, buildings.terrain_name AS building_terrain, " // building columns
			+ "npc.npc_id, npc.iff_template AS iff, npc.npc_name, npc.combat_level, npc.difficulty, npc.attackable, npc.faction, npc.spec_force, " // npc columns
			+ "npc_stats.HP, npc_stats.Action, npc_stats.Boss_HP, npc_stats.Boss_Action, npc_stats.Elite_HP, npc_stats.Elite_Action "	// npc_stats columns
			+ "FROM static, buildings, npc, npc_stats "
			+ "WHERE buildings.building_id = static.building_id AND static.npc_id = npc.npc_id AND npc.combat_level = npc_stats.Level";
	private static final String IDLE_MOOD = "idle";
	
	private final ObjectManager objectManager;
	private final Map<Long, Spawner> spawnerMap;
	private final ScheduledExecutorService executorService;
	private final Random random;
	
	public SpawnerService(ObjectManager objectManager) {
		this.objectManager = objectManager;
		executorService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("spawner-service"));
		spawnerMap = new HashMap<>();
		random = new Random();
		
		registerForIntent(ConfigChangedIntent.class, cci -> handleConfigChangedIntent(cci));
		registerForIntent(DestroyObjectIntent.class, doi -> handleDestroyObjectIntent(doi));
	}
	
	@Override
	public boolean initialize() {
		if(DataManager.getConfig(ConfigFile.FEATURES).getBoolean("SPAWN-EGGS-ENABLED", true))
			loadSpawners();
		
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		executorService.shutdown();
		
		return super.terminate();
	}
	
	private void handleConfigChangedIntent(ConfigChangedIntent cci) {
		String newValue, oldValue;
		
		if (cci.getChangedConfig().equals(ConfigFile.FEATURES) && cci.getKey().equals("SPAWN-EGGS-ENABLED")) {
			newValue = cci.getNewValue();
			oldValue = cci.getOldValue();

			if (!newValue.equals(oldValue)) {
				if (Boolean.valueOf(newValue) && spawnerMap.isEmpty()) { // If nothing's been spawned, create it.
					loadSpawners();
				} else { // If anything's been spawned, delete it.
					removeSpawners();
				}
			}
		}
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject destroyedObject = doi.getObject();
		if (!(destroyedObject instanceof DefaultAIObject))
			return;
		
		Spawner spawner = spawnerMap.remove(destroyedObject.getObjectId());
		
		if (spawner == null) {
			Log.e("Killed AI object %s has no linked Spawner - it cannot respawn!", destroyedObject);
			return;
		}
		
		executorService.schedule(() -> spawnNPC(spawner), spawner.getRespawnDelay(), TimeUnit.SECONDS);
	}
	
	private void loadSpawners() {
		long startTime = StandardLog.onStartLoad("spawners");
		
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("spawn/static.db", "static", "building/buildings", "npc/npc", "npc/npc_stats")) {
			try (ResultSet set = spawnerDatabase.executeQuery(GET_ALL_SPAWNERS_SQL)) {
				Location loc = new Location();
				while (set.next()) {
					if (set.getBoolean("active")) {	// TODO temporary until dynamically enabled NPCs are supported
						loadSpawner(set, loc);
					}
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
		
		StandardLog.onEndLoad(spawnerMap.size(), "spawners", startTime);
	}
	
	private void loadSpawner(ResultSet set, Location loc) throws SQLException {
		Spawner spawner = new Spawner(set.getInt("spawn_id"));
		spawner.setCreatureId(set.getString("npc_id"));
		spawner.setIffTemplates(createTemplateList(set.getString("iff")));
		spawner.setCreatureName(set.getString("npc_name").intern());
		spawner.setCombatLevel(set.getShort("combat_level"));
		spawner.setSpawnerFlag(SpawnerFlag.valueOf(set.getString("attackable")));
		setRespawnDelay(spawner, set);
		setDifficulty(spawner, set);
		setMoodAnimation(spawner, set);
		setAiBehavior(spawner, set);
		setLocation(spawner, loc, set);
		setFaction(spawner, set);
		createEgg(spawner, set);
		
		int amount = set.getInt("amount");
		for (int i = 0; i < amount; i++) {
			spawnNPC(spawner);
		}
	}
	
	private void createEgg(Spawner spawner, ResultSet set) throws SQLException {
		SpawnerType spawnerType = SpawnerType.valueOf(set.getString("spawner_type"));
		SWGObject egg = ObjectCreator.createObjectFromTemplate(spawnerType.getObjectTemplate());
		egg.setContainerPermissions(ContainerPermissionsType.ADMIN);
		egg.moveToContainer(getCell(spawner.getSpawnerId(), set));
		egg.setLocation(spawner.getLocation());
		spawner.setSpawnerObject(egg);
		new ObjectCreatedIntent(egg).broadcast();
	}
	
	private void setRespawnDelay(Spawner spawner, ResultSet set) throws SQLException {
		int minRespawnDelay = set.getInt("min_spawn_time");
		int maxRespawnDelay = set.getInt("max_spawn_time");
		
		if (minRespawnDelay > maxRespawnDelay) {
			Log.e("Spawner with ID %d has a minimum respawn time larger than the maximum respawn time", spawner.getSpawnerId());
			maxRespawnDelay = minRespawnDelay;
		}
		spawner.setMinRespawnDelay(minRespawnDelay);
		spawner.setMaxRespawnDelay(maxRespawnDelay);
	}
	
	private void setDifficulty(Spawner spawner, ResultSet set) throws SQLException {
		char difficultyChar = set.getString("difficulty").charAt(0);
		CreatureDifficulty difficulty;
		int maxHealth = 0;
		int maxAction = 0;
		
		switch (difficultyChar) {
			default:
				Log.w("Unknown creature difficulty: %s", difficultyChar);
			case 'N':
				difficulty = CreatureDifficulty.NORMAL;
				maxHealth = set.getInt("HP");
				maxAction = set.getInt("Action");
				break;
			case 'E':
				difficulty = CreatureDifficulty.ELITE;
				maxHealth = set.getInt("Elite_HP");
				maxAction = set.getInt("Elite_Action");
				break;
			case 'B':
				difficulty = CreatureDifficulty.BOSS;
				maxHealth = set.getInt("Boss_HP");
				maxAction = set.getInt("Boss_Action");
				break;
		}
		spawner.setMaxHealth(maxHealth);
		spawner.setMaxAction(maxAction);
		spawner.setCreatureDifficulty(difficulty);
	}
	
	private void setLocation(Spawner spawner, Location loc, ResultSet set) throws SQLException {
		Terrain terrain = Terrain.valueOf(set.getString("building_terrain"));
		loc.setTerrain(terrain);
		loc.setPosition(set.getFloat("x"), set.getFloat("y"), set.getFloat("z"));
		loc.setHeading(set.getFloat("heading"));
		spawner.setLocation(loc);
	}

	private void setFaction(Spawner spawner, ResultSet set) throws SQLException {
		String factionString = set.getString("faction");
		PvpFaction faction;

		switch (factionString) {
			case "rebel": faction = PvpFaction.REBEL; break;
			case "Imperial": faction = PvpFaction.IMPERIAL; break;
			default: return;
		}


		boolean specForce = set.getString("spec_force").equalsIgnoreCase("true");

		spawner.setFaction(faction, specForce);
	}
	
	private void setMoodAnimation(Spawner spawner, ResultSet set) throws SQLException {
		String moodAnimation = set.getString("mood").intern();
		
		if (moodAnimation == IDLE_MOOD) // since the string is intern'd, this will work
			moodAnimation = "neutral";
		spawner.setMoodAnimation(moodAnimation);
	}
	
	private void setAiBehavior(Spawner spawner, ResultSet set) throws SQLException {
		AIBehavior aiBehavior = AIBehavior.valueOf(set.getString("behaviour"));
		spawner.setAIBehavior(aiBehavior);
		if (aiBehavior == AIBehavior.FLOAT) {
			spawner.setFloatRadius(set.getInt("float_radius"));
		}
	}
	
	private SWGObject getCell(int spawnId, ResultSet set) throws SQLException {
		int cellId = set.getInt("cell_id");
		long buildingId = set.getLong("building_id");
		
		if (buildingId != 0 && cellId == 0) {
			Log.e("No cell ID specified for spawner with ID %d", spawnId);
			return null;
		} else if (buildingId == 0) {
			if (cellId != 0)
				Log.w("Unnecessary cell ID specified for spawner with ID %d", spawnId);
			return null; // No cell to find
		}
		
		SWGObject building = objectManager.getObjectById(buildingId);
		if (!(building instanceof BuildingObject)) {
			Log.w("Skipping spawner with ID %d - building_id %d didn't reference a BuildingObject!", spawnId, buildingId);
			return null;
		}
		
		SWGObject cellObject = ((BuildingObject) building).getCellByNumber(cellId);
		if (cellObject == null) {
			Log.e("Spawner with ID %d - building %d didn't have cell ID %d!", spawnId, buildingId, cellId);
		}
		return cellObject;
	}
	
	private void spawnNPC(Spawner spawner) {
		spawnerMap.put(createNPC(spawner), spawner);
	}
	
	private long createNPC(Spawner spawner) {
		DefaultAIObject object = ObjectCreator.createObjectFromTemplate(spawner.getRandomIffTemplate(), DefaultAIObject.class);
		
		object.setLocation(behaviorLocation(spawner));
		object.setObjectName(spawner.getCreatureName());
		object.setLevel(spawner.getCombatLevel());
		object.setDifficulty(spawner.getCreatureDifficulty());
		object.setMaxHealth(spawner.getMaxHealth());
		object.setHealth(spawner.getMaxHealth());
		object.setMaxAction(spawner.getMaxAction());
		object.setAction(spawner.getMaxAction());
		object.setMoodAnimation(spawner.getMoodAnimation());
		object.setBehavior(spawner.getAIBehavior());
		object.setFloatRadius(spawner.getFloatRadius());
		object.setCreatureId(spawner.getCreatureId());
		setFlags(object, spawner.getSpawnerFlag());
		setNPCFaction(object, spawner.getFaction(), spawner.isSpecForce());
		
		object.moveToContainer(spawner.getSpawnerObject().getParent());
		new ObjectCreatedIntent(object).broadcast();
		return object.getObjectId();
	}
	
	private void setFlags(CreatureObject creature, SpawnerFlag flags) {
		switch (flags) {
			case AGGRESSIVE:
				creature.setPvpFlags(PvpFlag.AGGRESSIVE);
				creature.addOptionFlags(OptionFlag.AGGRESSIVE);
			case ATTACKABLE:
				creature.setPvpFlags(PvpFlag.ATTACKABLE);
				creature.addOptionFlags(OptionFlag.HAM_BAR);
				break;
			case INVULNERABLE:
				creature.addOptionFlags(OptionFlag.INVULNERABLE);
				break;
		}
	}

	private void setNPCFaction(TangibleObject object, PvpFaction faction, boolean specForce) {
		if (faction == null) {
			return;
		}

		// Clear any existing flags that mark them as attackable
		object.clearPvpFlags(PvpFlag.ATTACKABLE, PvpFlag.AGGRESSIVE);
		object.removeOptionFlags(OptionFlag.AGGRESSIVE);

		new FactionIntent(object, faction).broadcast();

		if (specForce) {
			new FactionIntent(object, PvpStatus.SPECIALFORCES).broadcast();
		}
	}
	
	private String [] createTemplateList(String templates) {
		String [] templateList = templates.split(";");
		for (int i = 0; i < templateList.length; ++i) {
			templateList[i] = ClientFactory.formatToSharedFile("object/mobile/"+templateList[i]);
		}
		return templateList;
	}
	
	private void removeSpawners() {
		spawnerMap.values().forEach(spawner -> new DestroyObjectIntent(spawner.getSpawnerObject()).broadcast());
		spawnerMap.clear();
	}
	
	private Location behaviorLocation(Spawner spawner) {
		Location aiLocation = new Location(spawner.getLocation());
		
		switch (spawner.getAIBehavior()) {
			case FLOAT:
				// Random location within float radius of spawner and 
				int floatRadius = spawner.getFloatRadius();
				int offsetX = randomBetween(0, floatRadius);
				int offsetZ = randomBetween(0, floatRadius);
				
				spawner.setFloatRadius(floatRadius);
				aiLocation.setPosition(aiLocation.getX() + offsetX, aiLocation.getY(), aiLocation.getZ() + offsetZ);
	
				// Doesn't break here - FLOAT NPCs also have GUARD behavior
			case GUARD:
				// Random heading when spawned
				int randomHeading = randomBetween(0, 360);	// Can't use negative numbers as minimum
				aiLocation.setHeading(randomHeading);	// -180 to 180
				break;
			default:
				break;
		}
		
		return aiLocation;
	}
	
	/**
	 * Generates a random number between from (inclusive) and to (inclusive)
	 * @param from a positive minimum value
	 * @param to maximum value, which is larger than the minimum value
	 * @return a random number between the two, both inclusive
	 */
	private int randomBetween(int from, int to) {
		return random.nextInt((to - from) + 1) + from;
	}
}
