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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.projectswg.common.concurrency.PswgScheduledThreadPool;
import com.projectswg.common.control.Service;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Location.LocationBuilder;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.debug.Log;

import intents.FactionIntent;
import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.server.ConfigChangedIntent;
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
import resources.server_info.loader.BuildingLoader;
import resources.server_info.loader.BuildingLoader.BuildingLoaderInfo;
import resources.server_info.loader.npc.NpcLoader;
import resources.server_info.loader.npc.NpcLoader.NpcInfo;
import resources.server_info.loader.npc.NpcStatLoader;
import resources.server_info.loader.npc.NpcStatLoader.DetailNpcStatInfo;
import resources.server_info.loader.npc.NpcStatLoader.NpcStatInfo;
import resources.server_info.loader.spawn.StaticSpawnLoader;
import resources.server_info.loader.spawn.StaticSpawnLoader.StaticSpawnInfo;
import resources.spawn.Spawner;
import resources.spawn.Spawner.SpawnerFlag;
import resources.spawn.SpawnerType;
import services.objects.ObjectCreator;
import services.objects.ObjectManager.ObjectLookup;

public final class SpawnerService extends Service {
	
	private static final String IDLE_MOOD = "idle".intern();
	
	private final Map<Long, Spawner> spawnerMap;
	private final PswgScheduledThreadPool executor;
	private final Random random;
	
	public SpawnerService() {
		this.spawnerMap = new HashMap<>();
		this.executor = new PswgScheduledThreadPool(1, "spawner-service");
		this.random = new Random();
		
		registerForIntent(ConfigChangedIntent.class, cci -> handleConfigChangedIntent(cci));
		registerForIntent(DestroyObjectIntent.class, doi -> handleDestroyObjectIntent(doi));
	}
	
	@Override
	public boolean initialize() {
		executor.start();
		if(DataManager.getConfig(ConfigFile.FEATURES).getBoolean("SPAWN-EGGS-ENABLED", true))
			loadSpawners();
		
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		executor.stop();
		
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
		
		executor.execute(spawner.getRespawnDelay() * 1000, () -> spawnNPC(spawner));
	}
	
	private void loadSpawners() {
		long startTime = StandardLog.onStartLoad("spawners");
		
		BuildingLoader buildingLoader = BuildingLoader.load();
		NpcStatLoader npcStatLoader = NpcStatLoader.load();
		NpcLoader npcLoader = NpcLoader.load();
		StaticSpawnLoader staticSpawnLoader = StaticSpawnLoader.load();
		staticSpawnLoader.iterate(spawn -> {
			BuildingLoaderInfo building = buildingLoader.getBuilding(spawn.getBuildingId());
			if (building == null) {
				Log.e("Invalid entry for spawn id [%d] - unknown building: '%s'", spawn.getId(), spawn.getBuildingId());
				return;
			}
			
			NpcInfo npc = npcLoader.getNpc(spawn.getNpcId());
			if (npc == null) {
				Log.w("Invalid entry for spawn id [%d] - unknown NPC: '%s'", spawn.getId(), spawn.getNpcId());
				return;
			}
			
			NpcStatInfo npcStat = npcStatLoader.getNpcStats(npc.getCombatLevel());
			if (npcStat == null) {
				Log.w("Invalid entry for spawn id [%d], NPC id [%d] - unknown NPC stat for level: %d", spawn.getId(), npc.getId(), npc.getCombatLevel());
				return;
			}
			
			loadSpawner(spawn, building, npc, npcStat);
		});
		
		StandardLog.onEndLoad(spawnerMap.size(), "spawners", startTime);
	}
	
	private void loadSpawner(StaticSpawnInfo spawn, BuildingLoaderInfo building, NpcInfo npc, NpcStatInfo npcStat) {
		Spawner spawner = new Spawner(spawn.getId());
		spawner.setCreatureId(npc.getId());
		spawner.setIffTemplates(createTemplateList(npc.getIff()));
		spawner.setCreatureName(npc.getName().intern());
		spawner.setCombatLevel((short) npc.getCombatLevel());
		spawner.setSpawnerFlag(SpawnerFlag.valueOf(npc.getAttackable()));
		setRespawnDelay(spawner, spawn);
		setDifficulty(spawner, npc, npcStat);
		setMoodAnimation(spawner, spawn);
		setAiBehavior(spawner, spawn);
		setLocation(spawner, spawn, building);
		setFaction(spawner, npc);
		createEgg(spawner, spawn, building);
		
		for (int i = 0; i < spawn.getAmount(); i++) {
			spawnNPC(spawner);
		}
	}
	
	private void createEgg(Spawner spawner, StaticSpawnInfo spawn, BuildingLoaderInfo building) {
		SpawnerType spawnerType = SpawnerType.valueOf(spawn.getSpawnerType());
		SWGObject egg = ObjectCreator.createObjectFromTemplate(spawnerType.getObjectTemplate());
		egg.setContainerPermissions(ContainerPermissionsType.ADMIN);
		egg.moveToContainer(getCell(spawner.getSpawnerId(), spawn.getCellId(), building));
		egg.setLocation(spawner.getLocation());
		spawner.setSpawnerObject(egg);
		ObjectCreatedIntent.broadcast(egg);
	}
	
	private void setRespawnDelay(Spawner spawner, StaticSpawnInfo spawn) {
		int minRespawnDelay = spawn.getMinSpawnTime();
		int maxRespawnDelay = spawn.getMaxSpawnTime();
		
		if (minRespawnDelay > maxRespawnDelay) {
			Log.e("Spawner with ID %d has a minimum respawn time larger than the maximum respawn time", spawner.getSpawnerId());
			maxRespawnDelay = minRespawnDelay;
		}
		spawner.setMinRespawnDelay(minRespawnDelay);
		spawner.setMaxRespawnDelay(maxRespawnDelay);
	}
	
	private void setDifficulty(Spawner spawner, NpcInfo npc, NpcStatInfo npcStat) {
		char difficultyChar = npc.getDifficulty().charAt(0);
		
		CreatureDifficulty difficulty;
		DetailNpcStatInfo detailInfo; // undefined to trigger warnings if not defined below
		switch (difficultyChar) {
			default:
				Log.w("Unknown creature difficulty: %s", difficultyChar);
			case 'N':
				difficulty = CreatureDifficulty.NORMAL;
				detailInfo = npcStat.getNormalDetailStat();
				break;
			case 'E':
				difficulty = CreatureDifficulty.ELITE;
				detailInfo = npcStat.getEliteDetailStat();
				break;
			case 'B':
				difficulty = CreatureDifficulty.BOSS;
				detailInfo = npcStat.getBossDetailStat();
				break;
		}
		spawner.setMaxHealth(detailInfo.getHealth());
		spawner.setMaxAction(detailInfo.getAction());
		spawner.setCreatureDifficulty(difficulty);
	}
	
	private void setLocation(Spawner spawner, StaticSpawnInfo spawn, BuildingLoaderInfo building) {
		Location loc = Location.builder()
				.setTerrain(building.getTerrain())
				.setPosition(spawn.getX(), spawn.getY(), spawn.getZ())
				.setHeading(spawn.getHeading())
				.build();
		spawner.setLocation(loc);
	}

	private void setFaction(Spawner spawner, NpcInfo npc) {
		PvpFaction faction;
		
		switch (npc.getFaction()) {
			case "rebel":		faction = PvpFaction.REBEL; break;
			case "imperial":	faction = PvpFaction.IMPERIAL; break;
			default: return;
		}
		
		spawner.setFaction(faction, npc.isSpecForce());
	}
	
	private void setMoodAnimation(Spawner spawner, StaticSpawnInfo spawn) {
		String moodAnimation = spawn.getMood().intern();
		
		if (moodAnimation.equals(IDLE_MOOD))
			moodAnimation = "neutral";
		
		spawner.setMoodAnimation(moodAnimation);
	}
	
	private void setAiBehavior(Spawner spawner, StaticSpawnInfo spawn) {
		AIBehavior aiBehavior = AIBehavior.valueOf(spawn.getBehavior());
		spawner.setAIBehavior(aiBehavior);
		if (aiBehavior == AIBehavior.LOITER) {
			spawner.setFloatRadius(spawn.getLoiterRadius());
		}
	}
	
	private SWGObject getCell(int spawnId, int cellId, BuildingLoaderInfo buildingInfo) {
		if (buildingInfo.getId() != 0 && cellId == 0) {
			Log.e("No cell ID specified for spawner with ID %d", spawnId);
			return null;
		} else if (buildingInfo.getId() == 0) {
			if (cellId != 0)
				Log.w("Unnecessary cell ID specified for spawner with ID %d", spawnId);
			return null; // No cell to find
		}
		
		SWGObject building = ObjectLookup.getObjectById(buildingInfo.getId());
		if (!(building instanceof BuildingObject)) {
			Log.w("Skipping spawner with ID %d - building_id %d didn't reference a BuildingObject!", spawnId, buildingInfo.getId());
			return null;
		}
		
		SWGObject cellObject = ((BuildingObject) building).getCellByNumber(cellId);
		if (cellObject == null) {
			Log.e("Spawner with ID %d - building %d didn't have cell ID %d!", spawnId, buildingInfo.getId(), cellId);
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
		object.setLoiterRadius(spawner.getFloatRadius());
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
		LocationBuilder builder = Location.builder(spawner.getLocation());
		
		switch (spawner.getAIBehavior()) {
			case LOITER:
				// Random location within float radius of spawner and 
				int floatRadius = spawner.getFloatRadius();
				int offsetX = randomBetween(0, floatRadius);
				int offsetZ = randomBetween(0, floatRadius);
				
				spawner.setFloatRadius(floatRadius);
				builder.translatePosition(offsetX, 0, offsetZ);
	
				// Doesn't break here - LOITER NPCs also have TURN behavior
			case TURN:
				// Random heading when spawned
				int randomHeading = randomBetween(0, 360);	// Can't use negative numbers as minimum
				builder.setHeading(randomHeading);
				break;
			default:
				break;
		}
		
		return builder.build();
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
