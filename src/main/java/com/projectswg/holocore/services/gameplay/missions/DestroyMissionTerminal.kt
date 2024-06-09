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
package com.projectswg.holocore.services.gameplay.missions

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.PvpFaction
import com.projectswg.common.data.encodables.tangible.PvpFlag
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DestroyMissionLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.permissions.AdminPermissions
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.mission.MissionObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.services.gameplay.missions.DestroyMissionTerminalType.*
import me.joshlarson.jlcommon.log.Log
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class DestroyMissionTerminal(private val missionsToGenerate: Int, private val destroyMissionTerminalType: DestroyMissionTerminalType) {
	fun listMissions(player: Player): Collection<MissionListItem> {
		val creatureObject = player.creatureObject
		if (destroyMissionTerminalType == IMPERIAL && creatureObject.pvpFaction == PvpFaction.REBEL) {
			return emptyList()
		}
		if (destroyMissionTerminalType == REBEL && creatureObject.pvpFaction == PvpFaction.IMPERIAL) {
			return emptyList()
		}

		val playerWorldLocation = creatureObject.worldLocation
		val missionListItems = mutableListOf<MissionListItem>()

		for (destroyMissionInfo in randomDestroyMissionInfos(playerWorldLocation.terrain)) {
			val randomLocation = randomLocation(playerWorldLocation) ?: continue
			val difficulty = getDifficulty(playerWorldLocation.terrain)

			missionListItems.add(
				MissionListItem(
					location = randomLocation,
					creator = destroyMissionInfo.creator,
					difficulty = difficulty,
					target = destroyMissionInfo.target,
					title = StringId(destroyMissionInfo.stringFile, destroyMissionInfo.titleKey),
					description = StringId(destroyMissionInfo.stringFile, destroyMissionInfo.descriptionKey),
					reward = randomReward(difficulty),
					targetIff = getLairIffTemplate(destroyMissionInfo.dynamicId),
					serverAttribute = ServerAttribute.DYNAMIC_ID to destroyMissionInfo.dynamicId
				)
			)
		}

		return missionListItems
	}

	fun acceptMission(missionObject: MissionObject): TangibleObject {
		spawnNpcs(missionObject)
		return createLair(missionObject)
	}

	private fun spawnNpcs(missionObject: MissionObject) {
		val location = missionObject.startLocation.toLocation()
		val difficulty = missionObject.difficulty
		val egg = ObjectCreator.createObjectFromTemplate(SpawnerType.MISSION_EASY.objectTemplate)
		egg.containerPermissions = AdminPermissions.getPermissions()
		egg.moveToContainer(null, location)
		ObjectCreatedIntent(egg).broadcast()

		val dynamicId = missionObject.getServerTextAttribute(ServerAttribute.DYNAMIC_ID)
		val dynamicSpawnInfo = ServerData.dynamicSpawns.getSpawnInfo(dynamicId)
		val npcId = listOfNotNull(
			dynamicSpawnInfo?.npcNormal1, dynamicSpawnInfo?.npcNormal2, dynamicSpawnInfo?.npcNormal3, dynamicSpawnInfo?.npcNormal4
		).filter { it.isNotBlank() }.random()

		val spawnInfo = SimpleSpawnInfo.builder()
			.withNpcId(npcId)
			.withDifficulty(CreatureDifficulty.NORMAL)
			.withSpawnerFlag(NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE)
			.withMinLevel(difficulty)
			.withMaxLevel(difficulty)
			.withLocation(location)
			.withAmount(3)
			.withBehavior(AIBehavior.LOITER)
			.build()

		NPCCreator.createAllNPCs(Spawner(spawnInfo, egg))
	}

	private fun createLair(missionObject: MissionObject): TangibleObject {
		val location = missionObject.startLocation.toLocation()
		val lair = ObjectCreator.createObjectFromTemplate(missionObject.targetAppearance.string) as TangibleObject
		lair.removeOptionFlags(OptionFlag.INVULNERABLE)
		lair.setPvpFlags(PvpFlag.YOU_CAN_ATTACK)
		val lairFaction = when (destroyMissionTerminalType) {
			GENERAL  -> ServerData.factions.getFaction("neutral")
			REBEL    -> ServerData.factions.getFaction("imperial")
			IMPERIAL -> ServerData.factions.getFaction("rebel")
		}
		UpdateFactionIntent(lair, lairFaction ?: throw NullPointerException("Invalid mission terminal type $destroyMissionTerminalType")).broadcast()
		lair.moveToContainer(null, location)
		ObjectCreatedIntent(lair).broadcast()
		return lair
	}

	private fun randomDestroyMissionInfos(terrain: Terrain): Collection<DestroyMissionLoader.DestroyMissionInfo> {
		val destroyMissions = when (destroyMissionTerminalType) {
			GENERAL  -> ServerData.destroyMissions.getGeneralDestroyMissions(terrain)
			REBEL    -> ServerData.destroyMissions.getRebelDestroyMissions(terrain)
			IMPERIAL -> ServerData.destroyMissions.getImperialDestroyMissions(terrain)
		}

		return destroyMissions.shuffled().take(missionsToGenerate)
	}

	private fun randomLocation(base: Location): Location? {
		val distance = (1200 until 2500).random().toDouble()
		val direction = (0 until 360).random().toDouble()
		val alpha = Math.toRadians(direction)
		val xx = base.x + (distance * cos(alpha))
		val zz = base.z + (distance * sin(alpha))
		val yy = ServerData.terrains.getHeight(base.terrain, xx, zz)

		val randomLocation = Location.builder(base).setX(xx).setZ(zz).setY(yy).build()

		if (ServerData.noSpawnZones.isInNoSpawnZone(randomLocation)) {
			return null
		}

		return randomLocation
	}

	private fun getDifficulty(terrain: Terrain): Int {
		val terrainLevelInfo = ServerData.terrainLevels.getTerrainLevelInfo(terrain)

		if (terrainLevelInfo == null) {
			Log.w("Used fallback mission difficulty, as the terrain %s has no level info", terrain.getName())
			return 10
		}

		val minLevel = terrainLevelInfo.minLevel.toInt()
		val maxLevel = terrainLevelInfo.maxLevel.toInt()

		return (minLevel until maxLevel).random()
	}

	private fun getLairIffTemplate(dynamicId: String): String {
		val spawnInfo = ServerData.dynamicSpawns.getSpawnInfo(dynamicId)
		val fallbackLairTemplate = "object/tangible/lair/baz_nitch/shared_lair_baz_nitch.iff"

		if (spawnInfo == null) {
			Log.w("Unable to find dynamic spawn info for dynamicId $dynamicId, using fallback lair template")
			return fallbackLairTemplate
		}

		val randomLairId = spawnInfo.lairIds.random()
		val dynamicLairInfo = ServerData.dynamicLairs.getDynamicLairInfo(randomLairId)

		if (dynamicLairInfo == null) {
			Log.w("Unable to find dynamic lair info for lairId ${randomLairId}, using fallback lair template")
			return fallbackLairTemplate
		}

		return ClientFactory.formatToSharedFile(dynamicLairInfo.iffTemplate)
	}

	private fun randomReward(difficulty: Int): Int {
		val base = difficulty * 100.0
		val multiplier = (-5 until 5).random().div(100.0).plus(1.0)

		return (base * multiplier).toInt()
	}
}