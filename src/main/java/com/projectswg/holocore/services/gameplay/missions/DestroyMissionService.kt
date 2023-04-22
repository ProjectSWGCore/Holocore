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
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionAbort
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionAcceptRequest
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionAcceptResponse
import com.projectswg.common.network.packets.swg.zone.object_controller.MissionListRequest
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.resources.support.objects.swg.mission.MissionObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import com.projectswg.holocore.services.gameplay.missions.DestroyMissionTerminalType.*
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class DestroyMissionService : Service() {

	private val maxAcceptedMissions = 2
	private val missionsToGenerate = 5
	private val lairToMission = mutableMapOf<TangibleObject, MissionObject>()
	private val missionComplete = StringId("mission/mission_generic", "success_w_amount")

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

		when (packet) {
			is MissionListRequest   -> handleMissionListRequest(packet, player)
			is MissionAcceptRequest -> handleMissionAcceptRequest(packet, player)
			is MissionAbort         -> handleMissionAbort(packet, player)
		}
	}

	private fun handleMissionAbort(request: MissionAbort, player: Player) {
		val missionObjectId = request.missionObjectId
		val creatureObject = player.creatureObject
		val missionObject = ObjectLookup.getObjectById(missionObjectId) as MissionObject

		if (missionObject.parent != creatureObject.datapad) {
			return
		}

		val lairs = lairToMission.filterValues { it == missionObject }.keys
		lairs.forEach { lairToMission.remove(it) }
		DestroyObjectIntent.broadcast(missionObject)

		val response = MissionAbort(creatureObject.objectId)
		response.missionObjectId = missionObjectId
		creatureObject.sendSelf(response)
	}

	@IntentHandler
	private fun handleDestroyObject(destroyObjectIntent: DestroyObjectIntent) {
		val destroyedObject = destroyObjectIntent.`object`
		val missionObject = lairToMission.remove(destroyedObject)
		if (missionObject != null) {
			val owner = missionObject.owner
			if (owner != null) {
				handleMissionCompleted(owner, missionObject, destroyedObject.location)
			}
		}
	}

	private fun handleMissionCompleted(owner: Player, missionObject: MissionObject, location: Location) {
		DestroyObjectIntent.broadcast(missionObject)
		val groupId = owner.creatureObject.groupId
		val grouped = groupId != 0L

		if (grouped) {
			val groupObject = ObjectLookup.getObjectById(groupId) as GroupObject
			nearbyGroupMembers(groupObject, location).forEach { grantMissionReward(it, missionObject) }
		} else {
			grantMissionReward(owner, missionObject)
		}
	}

	private fun nearbyGroupMembers(groupObject: GroupObject, location: Location): List<Player> {
		return groupObject.groupMemberObjects.mapNotNull { it.owner }.filter { it.creatureObject.location.isWithinFlatDistance(location, 300.0) }
	}

	private fun grantMissionReward(player: Player, missionObject: MissionObject) {
		StandardLog.onPlayerEvent(this, player, "completed %s", missionObject)
		val reward = missionObject.reward
		player.creatureObject.addToBank(reward.toLong())
		SystemMessageIntent.broadcastPersonal(player, ProsePackage(missionComplete, "DI", reward))
	}

	private fun handleMissionAcceptRequest(missionAcceptRequest: MissionAcceptRequest, player: Player) {
		val missionTerminal = getDestroyMissionTerminal(missionAcceptRequest.terminalId) ?: return
		val missionId = missionAcceptRequest.missionId
		val missionObject = ObjectLookup.getObjectById(missionId) as MissionObject? ?: return
		val creatureObject = player.creatureObject
		val missionBag = creatureObject.missionBag
		val datapad = creatureObject.datapad

		if (datapad.containedObjects.filterIsInstance<MissionObject>().size >= maxAcceptedMissions) {
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

		val lair = missionTerminal.acceptMission(missionObject)
		lairToMission[lair] = missionObject
		StandardLog.onPlayerEvent(this, player, "accepted %s", missionObject)
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
		val missionTerminal = getDestroyMissionTerminal(missionListRequest.terminalId) ?: return
		val tickCount = missionListRequest.tickCount
		val creatureObject = player.creatureObject
		val missionBag = creatureObject.missionBag
		synchronizeMissionObjects(missionBag)
		val missionListItems = missionTerminal.listMissions(player)
		val containedObjects = missionBag.containedObjects
		val amountOfAvailableMissionObjects = containedObjects.size

		if (missionsToGenerate > amountOfAvailableMissionObjects) {
			throw IllegalStateException("Amount of missions to generate ($missionsToGenerate) was larger than the amount of available MissionObjects ($amountOfAvailableMissionObjects)")
		}

		val missionObjectIterator = containedObjects.iterator()
		for (missionListItem in missionListItems) {
			if (missionObjectIterator.hasNext()) {
				val missionObject = missionObjectIterator.next() as MissionObject
				updateMissionObject(missionObject, missionListItem)
				missionObject.tickCount = tickCount.toInt()
			}
		}
	}

	private fun updateMissionObject(missionObject: MissionObject, missionListItem: MissionListItem) {
		val (location, creator, difficulty, target, title, description, reward, targetIff, serverAttribute) = missionListItem
		missionObject.missionType = CRC("destroy")
		missionObject.missionCreator = creator
		missionObject.difficulty = difficulty
		missionObject.targetName = target
		missionObject.title = title
		missionObject.description = description
		missionObject.reward = reward
		missionObject.targetAppearance = CRC(targetIff)
		if (serverAttribute != null) {
			missionObject.setServerAttribute(serverAttribute.first, serverAttribute.second)
		}
		val missionLocation = MissionObject.MissionLocation()
		missionLocation.location = location.position
		missionLocation.terrain = location.terrain
		missionObject.startLocation = missionLocation
		missionObject.missionLocation = missionLocation
	}

	private fun getDestroyMissionTerminal(terminalId: Long): DestroyMissionTerminal? {
		val objectById = ObjectLookup.getObjectById(terminalId) ?: return null
		return when (objectById.template) {
			"object/tangible/terminal/shared_terminal_mission.iff"          -> DestroyMissionTerminal(missionsToGenerate, GENERAL)
			"object/tangible/terminal/shared_terminal_mission_rebel.iff"    -> DestroyMissionTerminal(missionsToGenerate, REBEL)
			"object/tangible/terminal/shared_terminal_mission_imperial.iff" -> DestroyMissionTerminal(missionsToGenerate, IMPERIAL)
			else                                                            -> null
		}
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

}