/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionAcceptRequest
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionAcceptResponse
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionListRequest
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
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
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.mission.MissionObject
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

class DestroyMissionService : Service() {

	private val maxAcceptedMissions = 2
	private val missionsToGenerate = 5
	private val npcToMission = mutableMapOf<AIObject, MissionObject>()

	@IntentHandler
	private fun handleObjectCreated(objectCreatedIntent: ObjectCreatedIntent) {
		val swgObject = objectCreatedIntent.`object`

		if ("object/tangible/mission_bag/shared_mission_bag.iff" == swgObject.template) {
			synchronizeMissionObjects(swgObject)
		}
	}

	@IntentHandler
	private fun handleInboundPacket(inboundPacketIntent: InboundPacketIntent) {
		val packet = inboundPacketIntent.packet
		val player = inboundPacketIntent.player

		if (packet is MissionListRequest) {
			handleMissionListRequest(packet, player)
		} else if (packet is MissionAcceptRequest) {
			handleMissionAcceptRequest(packet, player)
		}
	}

	@IntentHandler
	private fun handleCreatureKilled(creatureKilledIntent: CreatureKilledIntent) {
		val corpse = creatureKilledIntent.corpse
		val missionObject = npcToMission.remove(corpse)
		if (missionObject != null) {
			val owner = missionObject.owner
			if (owner != null) {
				handleMissionCompleted(owner, missionObject)
			}
		}
	}

	private fun handleMissionCompleted(owner: Player, missionObject: MissionObject) {
		StandardLog.onPlayerEvent(this, owner, "completed %s", missionObject)
		DestroyObjectIntent.broadcast(missionObject)
		val reward = missionObject.reward
		owner.creatureObject.addToBank(reward.toLong())
		val missionComplete = StringId("mission/mission_generic", "success_w_amount")
		SystemMessageIntent.broadcastPersonal(owner, ProsePackage(missionComplete, "DI", reward))
	}

	private fun handleMissionAcceptRequest(missionAcceptRequest: MissionAcceptRequest, player: Player) {
		if (!isDestroyMissionTerminal(missionAcceptRequest.terminalId)) {
			return
		}

		val missionId = missionAcceptRequest.missionId
		val missionObject = ObjectLookup.getObjectById(missionId) as MissionObject?

		if (missionObject != null) {
			val creatureObject = player.creatureObject
			val missionBag = creatureObject.missionBag
			val datapad = creatureObject.datapad

			if (datapad.containedObjects.size >= maxAcceptedMissions) {
				handleTooManyMissions(creatureObject, missionId, missionAcceptRequest, player)
				return
			}

			if (!missionBag.containedObjects.contains(missionObject)) {
				StandardLog.onPlayerError(this, player, "requested to accept mission not in their mission_bag")
				return
			}


			val missionAcceptResponse = MissionAcceptResponse(creatureObject.objectId)
			missionAcceptResponse.missionObjectId = missionId
			missionAcceptResponse.terminalType = missionAcceptRequest.terminalType.toInt()
			missionAcceptResponse.success = 1
			creatureObject.sendSelf(missionAcceptResponse)

			missionObject.moveToContainer(datapad)
			val location = missionObject.startLocation.toLocation()
			missionObject.waypointPackage = createWaypoint(location)

			spawnNpc(location, missionObject)
			StandardLog.onPlayerEvent(this, player, "accepted %s", missionObject)
		}
	}

	private fun spawnNpc(location: Location, missionObject: MissionObject) {
		val difficulty = missionObject.difficulty
		val egg = ObjectCreator.createObjectFromTemplate(SpawnerType.MISSION_EASY.objectTemplate)
		egg.containerPermissions = AdminPermissions.getPermissions()
		egg.moveToContainer(null, location)
		ObjectCreatedIntent.broadcast(egg)

		val dynamicId = missionObject.getServerTextAttribute(ServerAttribute.DYNAMIC_ID)
		val dynamicSpawnInfo = ServerData.dynamicSpawns.getSpawnInfo(dynamicId)
		val npcId = listOfNotNull(dynamicSpawnInfo?.npcNormal1, dynamicSpawnInfo?.npcNormal2, dynamicSpawnInfo?.npcNormal3, dynamicSpawnInfo?.npcNormal4)
			.filter { it.isNotBlank() }
			.random()

		val spawnInfo = SimpleSpawnInfo.builder()
			.withNpcId(npcId)
			.withDifficulty(CreatureDifficulty.NORMAL)
			.withSpawnerFlag(NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE)
			.withMinLevel(difficulty)
			.withMaxLevel(difficulty)
			.withLocation(location)
			.build()

		val npc = NPCCreator.createNPC(Spawner(spawnInfo, egg))
		npcToMission[npc] = missionObject
	}

	private fun handleTooManyMissions(creatureObject: CreatureObject, missionId: Long, missionAcceptRequest: MissionAcceptRequest, player: Player) {
		val missionAcceptResponse = MissionAcceptResponse(creatureObject.objectId)
		missionAcceptResponse.missionObjectId = missionId
		missionAcceptResponse.terminalType = missionAcceptRequest.terminalType.toInt()
		missionAcceptResponse.success = 0
		creatureObject.sendSelf(missionAcceptResponse)
		SystemMessageIntent.broadcastPersonal(player, ProsePackage("mission/mission_generic", "too_many_missions"))
	}

	private fun createWaypoint(location: Location): WaypointPackage {
		val waypoint = ObjectCreator.createObjectFromTemplate("object/waypoint/shared_waypoint.iff") as WaypointObject
		waypoint.setPosition(location.terrain, location.x, location.y, location.z)
		waypoint.color = WaypointColor.YELLOW
		ObjectCreatedIntent(waypoint).broadcast()
		return waypoint.oob
	}

	private fun handleMissionListRequest(missionListRequest: MissionListRequest, player: Player) {
		if (!isDestroyMissionTerminal(missionListRequest.terminalId)) {
			return
		}

		val tickCount = missionListRequest.tickCount
		val creatureObject = player.creatureObject
		val missionBag = creatureObject.missionBag

		synchronizeMissionObjects(missionBag)
		generateMissions(missionBag, creatureObject, tickCount)
	}

	private fun isDestroyMissionTerminal(terminalId: Long): Boolean {
		val objectById = ObjectLookup.getObjectById(terminalId)

		return objectById?.template == "object/tangible/terminal/shared_terminal_mission.iff"
	}

	private fun generateMissions(missionBag: SWGObject, creatureObject: CreatureObject, tickCount: Byte) {
		val containedObjects = missionBag.containedObjects
		val amountOfAvailableMissionObjects = containedObjects.size

		if (missionsToGenerate > amountOfAvailableMissionObjects) {
			throw IllegalStateException("Amount of missions to generate ($missionsToGenerate) was larger than the amount of available MissionObjects ($amountOfAvailableMissionObjects)")
		}

		val missionObjectIterator = containedObjects.iterator()
		val location = creatureObject.location
		val randomDestroyMissionInfos = randomDestroyMissionInfos(location.terrain)

		for (randomDestroyMissionInfo in randomDestroyMissionInfos) {
			val missionObject = missionObjectIterator.next() as MissionObject
			val randomLocation = randomLocation(location)

			if (randomLocation != null) {
				updateMissionObject(missionObject, randomLocation, randomDestroyMissionInfo)
				missionObject.tickCount = tickCount.toInt()
			}
		}
	}

	private fun randomLocation(base: Location): Location? {
		val distance = (1200 until 2500).random()
			.toDouble()
		val direction = (0 until 360).random()
			.toDouble()
		val alpha = toRadians(direction)
		val xx = base.x + (distance * cos(alpha))
		val zz = base.z + (distance * sin(alpha))
		val yy = ServerData.terrains.getHeight(base.terrain, xx, zz)

		val randomLocation = Location.builder(base)
			.setX(xx)
			.setZ(zz)
			.setY(yy)
			.build()

		if (ServerData.noSpawnZones.isInNoSpawnZone(randomLocation)) {
			return null
		}

		return randomLocation
	}

	private fun randomDestroyMissionInfos(terrain: Terrain): Collection<DestroyMissionLoader.DestroyMissionInfo> {
		return ServerData.destroyMissions.getDestroyMissions(terrain)
			.shuffled()
			.take(missionsToGenerate)
	}

	private fun synchronizeMissionObjects(missionBag: SWGObject) {
		destroyExcessMissions(missionBag)
		createMissingMissions(missionBag)
	}

	private fun createMissingMissions(missionBag: SWGObject) {
		val actualAmountOfMissionObjects = missionBag.containedObjects.size
		val missionsToCreate = missionsToGenerate - actualAmountOfMissionObjects

		for (i in 1..missionsToCreate) {
			val missionObject = createMissionObject()
			missionObject.moveToContainer(missionBag)
		}
	}

	private fun destroyExcessMissions(missionBag: SWGObject) {
		val actualAmountOfMissions = missionBag.containedObjects.size
		val iterator = missionBag.containedObjects.iterator()
		for (i in missionsToGenerate until actualAmountOfMissions) {
			DestroyObjectIntent.broadcast(iterator.next())
		}
	}

	private fun createMissionObject(): MissionObject {
		val missionObject = ObjectCreator.createObjectFromTemplate("object/mission/shared_mission_object.iff") as MissionObject
		ObjectCreatedIntent.broadcast(missionObject)

		return missionObject
	}

	private fun updateMissionObject(missionObject: MissionObject, location: Location, destroyMissionInfo: DestroyMissionLoader.DestroyMissionInfo) {
		missionObject.missionType = CRC("destroy")
		missionObject.missionCreator = destroyMissionInfo.creator
		val difficulty = getDifficulty(location.terrain)
		missionObject.difficulty = difficulty
		missionObject.targetName = destroyMissionInfo.target
		missionObject.title = StringId(destroyMissionInfo.stringFile, destroyMissionInfo.titleKey)
		missionObject.description = StringId(destroyMissionInfo.stringFile, destroyMissionInfo.descriptionKey)
		missionObject.reward = randomReward(difficulty)
		val dynamicId = destroyMissionInfo.dynamicId
		missionObject.targetAppearance = CRC(getLairIffTemplate(dynamicId))
		missionObject.setServerAttribute(ServerAttribute.DYNAMIC_ID, dynamicId)
		val missionLocation = MissionObject.MissionLocation()
		missionLocation.location = location.position
		missionLocation.terrain = location.terrain
		missionObject.startLocation = missionLocation
		missionObject.missionLocation = missionLocation
	}

	private fun randomReward(difficulty: Int): Int {
		val base = difficulty * 100.0
		val multiplier = (-5 until 5).random()
			.div(100.0)
			.plus(1.0)
		
		return (base * multiplier).toInt()
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

}