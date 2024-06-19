/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.support.npc.spawn

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.intents.gameplay.world.CreateSpawnIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.resources.support.data.location.ClosestLocationReducer
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.terrains
import com.projectswg.holocore.resources.support.data.server_info.loader.NoSpawnZoneLoader.NoSpawnZoneInfo
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.dynamicSpawns
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.noSpawnZones
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.terrainLevels
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.SpawnerFlag
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ThreadLocalRandom

class DynamicSpawnService : Service() {
	private val dynamicSpawnLoader = dynamicSpawns
	private val noSpawnZoneLoader = noSpawnZones
	private val terrainLevelLoader = terrainLevels
	private val npcSpawnChance = config.getLong(this, "npcSpawnChance", 7) // Chance in % that a NPC is dynamically spawned when a player moves
	private val maxObservedNpcs = config.getLong(this, "maxObservedNpcs", 10) // A player should never see more than this amount of alive NPCs

	@IntentHandler
	private fun handlePlayerTransformed(intent: PlayerTransformedIntent) {
		val location = intent.newLocation

		spawnNewNpcs(intent.player, location)
	}

	@IntentHandler
	private fun handleDestroyObjectIntent(intent: DestroyObjectIntent) {
		val `object` = intent.obj

		if (`object` is AIObject) {
			val spawner: Spawner = `object`.spawner
			val egg = spawner.egg

			if (SPAWNER_TYPE.objectTemplate == egg.template) {
				// If the dynamic NPC dies, don't let it respawn to prevent overcrowding an area
				DestroyObjectIntent(egg).broadcast()
			}
		}
	}

	private fun spawnNewNpcs(player: CreatureObject, location: Location) {
		val terrain = location.terrain
		val spawnInfos = dynamicSpawnLoader.getSpawnInfos(terrain)

		if (spawnInfos.isEmpty()) {
			// There's nothing we can spawn on this planet. Do nothing.
			return
		}

		val terrainLevelInfo = terrainLevelLoader.getTerrainLevelInfo(terrain) ?: // Terrain has no level range defined, we can't spawn anything without
		return


		// Random chance to create a spawn
		val random = ThreadLocalRandom.current()
		val randomChance = random.nextInt(0, 100)

		if (randomChance > npcSpawnChance) {
			return
		}

		if (noSpawnZoneLoader.isInNoSpawnZone(location)) {
			// The player is in a no spawn zone. Don't spawn anything.
			return
		}

		val dynamicSpawnEggTemplate = SPAWNER_TYPE.objectTemplate

		// @formatter:off
		val dynamicSpawnsWithAliveNpcs = player.aware.stream()
			.filter { swgObject: SWGObject? -> swgObject is AIObject }
			.map { swgObject: SWGObject -> swgObject as AIObject }
			.map { obj: AIObject -> obj.spawner }
			.map(Spawner::egg)
			.map { obj: SWGObject -> obj.template }
			.filter { anObject: String? -> dynamicSpawnEggTemplate == anObject }
			.count()
		// @formatter:on

		if (dynamicSpawnsWithAliveNpcs >= maxObservedNpcs) {
			// Plenty spawns near this player already - do nothing
			return
		}


		// Find closest no spawn zone
		val noSpawnZoneInfos = noSpawnZoneLoader.getNoSpawnZoneInfos(terrain)

		if (!noSpawnZoneInfos.isEmpty()) {
			val closestZoneOpt = noSpawnZoneInfos.stream().map { noSpawnZoneInfo: NoSpawnZoneInfo ->
					Location.builder().setX(noSpawnZoneInfo.x.toDouble()).setZ(noSpawnZoneInfo.z.toDouble()).setTerrain(location.terrain).build()
				}.reduce(ClosestLocationReducer(location))

			val closestZoneLocation = closestZoneOpt.get()

			val tooCloseToNoSpawnZone = location.isWithinFlatDistance(closestZoneLocation, MAX_SPAWN_DISTANCE_TO_PLAYER.toDouble())

			if (tooCloseToNoSpawnZone) {
				// Player is too close to a no spawn zone. Don't spawn anything.
				return
			}
		}


		// Spawn the egg
		val randomOffsetX = random.nextDouble(-MAX_SPAWN_DISTANCE_TO_PLAYER.toDouble(), MAX_SPAWN_DISTANCE_TO_PLAYER.toDouble())
		val randomOffsetZ = random.nextDouble(-MAX_SPAWN_DISTANCE_TO_PLAYER.toDouble(), MAX_SPAWN_DISTANCE_TO_PLAYER.toDouble())
		val eggX = location.x + randomOffsetX
		val eggZ = location.z + randomOffsetZ
		val eggY = terrains().getHeight(terrain, eggX, eggZ)

		val eggLocation = Location.builder(location).setX(eggX).setZ(eggZ).setY(eggY).build()
		val randomSpawnInfoIndex = random.nextInt(0, spawnInfos.size)
		val spawnInfo = ArrayList(spawnInfos)[randomSpawnInfoIndex]

		val minLevel = terrainLevelInfo.minLevel.toInt()
		val maxLevel = terrainLevelInfo.maxLevel.toInt()

		val spawnerFlag = spawnInfo.spawnerFlag

		StandardLog.onPlayerEvent(this, player, "Spawning %s", spawnInfo.dynamicId)

		spawn(randomNpc(spawnInfo.npcBoss), CreatureDifficulty.BOSS, spawnerFlag, minLevel, maxLevel, eggLocation)
		spawn(randomNpc(spawnInfo.npcElite), CreatureDifficulty.ELITE, spawnerFlag, minLevel, maxLevel, eggLocation)
		spawn(randomNpc(spawnInfo.npcNormal1), CreatureDifficulty.NORMAL, spawnerFlag, minLevel, maxLevel, eggLocation)
		spawn(randomNpc(spawnInfo.npcNormal2), CreatureDifficulty.NORMAL, spawnerFlag, minLevel, maxLevel, eggLocation)
		spawn(randomNpc(spawnInfo.npcNormal3), CreatureDifficulty.NORMAL, spawnerFlag, minLevel, maxLevel, eggLocation)
		spawn(randomNpc(spawnInfo.npcNormal4), CreatureDifficulty.NORMAL, spawnerFlag, minLevel, maxLevel, eggLocation)
	}

	private fun spawn(npcId: String?, difficulty: CreatureDifficulty, spawnerFlag: SpawnerFlag, minLevel: Int, maxLevel: Int, location: Location) {
		if (npcId == null) {
			return
		}

		val simpleSpawnInfo = SimpleSpawnInfo.builder().withNpcId(npcId).withDifficulty(difficulty).withSpawnerType(SpawnerType.WAYPOINT_AUTO_SPAWN).withMinLevel(minLevel).withMaxLevel(maxLevel).withSpawnerFlag(spawnerFlag).withBehavior(AIBehavior.LOITER).withLocation(location).build()

		CreateSpawnIntent(simpleSpawnInfo).broadcast()
	}

	private fun randomNpc(npcString: String): String? {
		if (npcString.isEmpty()) {
			return null
		}

		val npcIds = npcString.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val npcIdCount = npcIds.size
		val random = ThreadLocalRandom.current()
		val randomIdx = random.nextInt(0, npcIdCount)

		return npcIds[randomIdx]
	}

	companion object {
		private const val MAX_SPAWN_DISTANCE_TO_PLAYER = 250 // Spawner is created up to this amount of meters away from the player
		private val SPAWNER_TYPE = SpawnerType.WAYPOINT_AUTO_SPAWN // Important that this type is only used by dynamic spawns
	}
}