/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/
package com.projectswg.holocore.services.support.npc.spawn;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.intents.gameplay.world.spawn.CreateSpawnIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.resources.support.data.location.ClosestLocationReducer;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DynamicSpawnLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.NoSpawnZoneLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.loader.TerrainLevelLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo;
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class DynamicSpawnService extends Service {
	
	private static final int SPAWN_DISTANCE_TO_PLAYER = 70;    // Spawner is created 70m away from the player and NPCs are spawned around the spawner
	private static final SpawnerType SPAWNER_TYPE = SpawnerType.RANDOM;
	
	private final DynamicSpawnLoader dynamicSpawnLoader;
	private final NoSpawnZoneLoader noSpawnZoneLoader;
	private final TerrainLevelLoader terrainLevelLoader;
	private final long spawnsPerArea;
	
	public DynamicSpawnService() {
		dynamicSpawnLoader = ServerData.INSTANCE.getDynamicSpawns();
		noSpawnZoneLoader = ServerData.INSTANCE.getNoSpawnZones();
		terrainLevelLoader = ServerData.INSTANCE.getTerrainLevels();
		spawnsPerArea = PswgDatabase.INSTANCE.getConfig().getLong(this, "eggsPerArea", 4) * 3;    // Amount of spawns in an area
	}
	
	@IntentHandler
	private void handlePlayerTransformed(PlayerTransformedIntent intent) {
		Location location = intent.getNewLocation();
		
		spawnNewNpcs(intent.getPlayer(), location);
	}
	
	private void spawnNewNpcs(CreatureObject player, Location location) {
		Terrain terrain = location.getTerrain();
		Collection<DynamicSpawnLoader.DynamicSpawnInfo> spawnInfos = dynamicSpawnLoader.getSpawnInfos(terrain);
		
		if (spawnInfos.isEmpty()) {
			// There's nothing we can spawn on this planet. Do nothing.
			return;
		}
		
		if (noSpawnZoneLoader.isInNoSpawnZone(location)) {
			// The player is in a no spawn zone. Don't spawn anything.
			return;
		}
		
		TerrainLevelLoader.TerrainLevelInfo terrainLevelInfo = terrainLevelLoader.getTerrainLevelInfo(terrain);
		
		if (terrainLevelInfo == null) {
			// Terrain has no level range defined
			return;
		}
		
		// Find closest no spawn zone
		Collection<NoSpawnZoneLoader.NoSpawnZoneInfo> noSpawnZoneInfos = noSpawnZoneLoader.getNoSpawnZoneInfos(terrain);
		
		if (!noSpawnZoneInfos.isEmpty()) {
			Optional<Location> closestZoneOpt = noSpawnZoneInfos.stream()
					.map(noSpawnZoneInfo -> Location.builder().setX(noSpawnZoneInfo.getX()).setZ(noSpawnZoneInfo.getZ())
							.setTerrain(location.getTerrain()).build()).reduce(new ClosestLocationReducer(location));
			
			Location closestZoneLocation = closestZoneOpt.get();
			
			boolean tooCloseToNoSpawnZone = location.isWithinFlatDistance(closestZoneLocation, SPAWN_DISTANCE_TO_PLAYER);
			
			if (tooCloseToNoSpawnZone) {
				// Player is too close to a no spawn zone. Don't spawn anything.
				return;
			}
		}
		
		String eggTemplate = SPAWNER_TYPE.getObjectTemplate();
		
		long nearbyEggs = player.getAware().stream().filter(swgObject -> eggTemplate.equals(swgObject.getTemplate())).count();
		
		if (nearbyEggs >= spawnsPerArea) {
			// Plenty spawns near this player already - do nothing
			return;
		}
		
		// Spawn the egg
		ThreadLocalRandom random = ThreadLocalRandom.current();
		boolean usePositiveDirectionX = random.nextBoolean();
		boolean usePositiveDirectionZ = random.nextBoolean();
		double eggX = (usePositiveDirectionX ? SPAWN_DISTANCE_TO_PLAYER : -SPAWN_DISTANCE_TO_PLAYER) + location.getX();
		double eggZ = (usePositiveDirectionZ ? SPAWN_DISTANCE_TO_PLAYER : -SPAWN_DISTANCE_TO_PLAYER) + location.getZ();
		Location eggLocation = Location.builder(location).setX(eggX).setZ(eggZ)
				.build();    // TODO y parameter should be set and calculated based on X and Z in relevant terrain. Currently they'll spawn in the air or below ground.
		int randomSpawnInfoIndex = random.nextInt(0, spawnInfos.size());
		DynamicSpawnLoader.DynamicSpawnInfo spawnInfo = new ArrayList<>(spawnInfos).get(randomSpawnInfoIndex);
		
		int minLevel = (int) terrainLevelInfo.getMinLevel();
		int maxLevel = (int) terrainLevelInfo.getMaxLevel();
		
		NpcStaticSpawnLoader.SpawnerFlag spawnerFlag = spawnInfo.getSpawnerFlag();
		
		StandardLog.onPlayerEvent(this, player, "Spawning %s", spawnInfo.getDynamicId());
		
		spawn(randomNpc(spawnInfo.getNpcBoss()), CreatureDifficulty.BOSS, spawnerFlag, minLevel, maxLevel, eggLocation);
		spawn(randomNpc(spawnInfo.getNpcElite()), CreatureDifficulty.ELITE, spawnerFlag, minLevel, maxLevel, eggLocation);
		spawn(randomNpc(spawnInfo.getNpcNormal1()), CreatureDifficulty.NORMAL, spawnerFlag, minLevel, maxLevel, eggLocation);
		spawn(randomNpc(spawnInfo.getNpcNormal2()), CreatureDifficulty.NORMAL, spawnerFlag, minLevel, maxLevel, eggLocation);
		spawn(randomNpc(spawnInfo.getNpcNormal3()), CreatureDifficulty.NORMAL, spawnerFlag, minLevel, maxLevel, eggLocation);
		spawn(randomNpc(spawnInfo.getNpcNormal4()), CreatureDifficulty.NORMAL, spawnerFlag, minLevel, maxLevel, eggLocation);
	}
	
	private void spawn(String npcId, CreatureDifficulty difficulty, NpcStaticSpawnLoader.SpawnerFlag spawnerFlag, int minLevel, int maxLevel, Location location) {
		if (npcId == null) {
			return;
		}
		
		SimpleSpawnInfo simpleSpawnInfo = SimpleSpawnInfo.builder().withNpcId(npcId).withDifficulty(difficulty).withSpawnerType(SpawnerType.RANDOM)
				.withMinLevel(minLevel).withMaxLevel(maxLevel).withSpawnerFlag(spawnerFlag).withBehavior(AIBehavior.LOITER).withLocation(location)
				.build();
		
		CreateSpawnIntent.broadcast(simpleSpawnInfo);
	}
	
	@Nullable
	private String randomNpc(String npcString) {
		if (npcString.isEmpty()) {
			return null;
		}
		
		String[] npcIds = npcString.split(";");
		int npcIdCount = npcIds.length;
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int randomIdx = random.nextInt(0, npcIdCount);
		
		return npcIds[randomIdx];
	}
	
}
