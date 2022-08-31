/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.services.support.npc.spawn

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Point3D
import com.projectswg.common.network.packets.swg.zone.CreateClientPathMessage
import com.projectswg.common.network.packets.swg.zone.DestroyClientPathMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.IntendedTarget
import com.projectswg.holocore.intents.gameplay.world.spawn.CreateSpawnIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.permissions.AdminPermissions
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

class SpawnerService : Service() {
	
	private val executor: ScheduledThreadPool = ScheduledThreadPool(1, "spawner-service")
	private val adminsWithRoutes: MutableSet<CreatureObject> = ConcurrentHashMap.newKeySet()
	
	override fun initialize(): Boolean {
		executor.start()
		if (PswgDatabase.config.getBoolean(this, "spawnEggsEnabled", true))
			loadSpawners()
		
		return true
	}
	
	override fun terminate(): Boolean {
		executor.stop()
		executor.awaitTermination(1000)
		return true
	}
	
	@IntentHandler
	private fun handleInboundPacketIntent(ipi: InboundPacketIntent) {
		val player = ipi.player.creatureObject
		val packet = ipi.packet
		if (packet !is IntendedTarget || player == null)
			return
		val intendedTargetId = packet.targetId
		val intendedTarget = ObjectLookup.getObjectById(intendedTargetId)
		if (intendedTarget == null && adminsWithRoutes.remove(player)) {
			player.sendSelf(DestroyClientPathMessage())
		} else if (intendedTarget != null && player.hasCommand("admin")) {
			val spawner = intendedTarget.getServerAttribute(ServerAttribute.EGG_SPAWNER) as Spawner?
			if (spawner != null) {
				val waypoints = spawner.patrolRoute
				if (waypoints != null) {
					player.sendSelf(CreateClientPathMessage(waypoints.stream()
							.map { wayp -> if (wayp.parent == null) wayp.location else Location.builder(wayp.location).translateLocation(wayp.parent.worldLocation).build() }
							.map { it.position }
							.collect(Collectors.toList<Point3D>())))
					adminsWithRoutes.add(player)
				}
			}
		}
	}
	
	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		val player = pei.player.creatureObject
		if (pei.event == PlayerEvent.PE_LOGGED_OUT && player != null && adminsWithRoutes.remove(player))
			player.sendSelf(DestroyClientPathMessage())
	}
	
	@IntentHandler
	private fun handleDestroyObjectIntent(doi: DestroyObjectIntent) {
		val obj = doi.`object`

		if (isNPC(obj)) {
			handleNPCDestroyed(obj)
		}

		if (isSpawner(obj)) {
			handleSpawnerDestroyed(obj)
		}
	}

	@IntentHandler
	private fun handleCreateSpawnIntent(csi: CreateSpawnIntent) {
		spawn(csi.spawnInfo)
	}

	private fun isNPC(obj: SWGObject): Boolean {
		return obj is AIObject
	}

	private fun isSpawner(obj: SWGObject): Boolean {
		return obj.getServerAttribute(ServerAttribute.EGG_SPAWNER) != null
	}

	private fun handleNPCDestroyed(obj: SWGObject) {
		val destroyedObject = obj as? AIObject ?: return

		val spawner = destroyedObject.spawner

		if (spawner == null) {
			Log.i("Killed AI object %s has no linked Spawner - it will not respawn", destroyedObject)
			return
		}

		spawner.removeNPC(destroyedObject)

		if (spawner.behavior != AIBehavior.PATROL || spawner.npcs.isEmpty()) {
			val respawnDelay = spawner.respawnDelay

			if (respawnDelay > 0) {
				executor.execute((respawnDelay * 1000).toLong()) { respawn(spawner) }
			} else {
				DestroyObjectIntent.broadcast(spawner.egg)
			}
		}
	}

	private fun handleSpawnerDestroyed(obj: SWGObject) {
		val spawner = obj.getServerAttribute(ServerAttribute.EGG_SPAWNER) as Spawner

		val npcs = spawner.npcs.toList()

		for (npc in npcs) {
			npc.spawner = null	// Prevents the NPC from respawning
			DestroyObjectIntent.broadcast(npc)
		}
	}

	private fun loadSpawners() {
		val startTime = StandardLog.onStartLoad("spawners")
		
		var count = 0
		for (spawn in ServerData.npcStaticSpawns.spawns) {
			try {
				spawn(spawn)
				count++
			} catch (t: Throwable) {
				Log.e("Failed to load spawner[%s]/npc[%s]. %s: %s", spawn.id, spawn.npcId, t.javaClass.name, t.message)
			}
			
		}
		
		StandardLog.onEndLoad(count, "spawners", startTime)
	}
	
	private fun spawn(spawn: SpawnInfo) {
		val npcId = spawn.npcId

		if (ServerData.npcs.getNpc(npcId) == null) {
			Log.w("Invalid npc %s", npcId)
			return
		}

		val egg = createEgg(spawn)
		val spawner = Spawner(spawn, egg)
		egg.setServerAttribute(ServerAttribute.EGG_SPAWNER, spawner)
		
		for (i in 0 until spawner.amount) {
			NPCCreator.createNPC(spawner)
		}
		
		val patrolRoute = spawner.patrolRoute
		if (patrolRoute != null) {
			for (waypoint in patrolRoute) {
				val obj = ObjectCreator.createObjectFromTemplate("object/path_waypoint/path_waypoint_patrol.iff")
				obj.containerPermissions = AdminPermissions.getPermissions()
				obj.setServerAttribute(ServerAttribute.EGG_SPAWNER, spawner)
				
				obj.objectName = String.format("P: %s\nG: %s\nNPC: %s\nID: %s", waypoint.patrolId, waypoint.groupId, npcId, spawn.id)
				obj.moveToContainer(waypoint.parent, waypoint.location)
				ObjectCreatedIntent.broadcast(obj)
			}
		}
	}
	
	private fun respawn(spawner: Spawner) {
		for (i in spawner.npcs.size until spawner.amount) {
			NPCCreator.createNPC(spawner)
		}
	}
	
	private fun createEgg(spawn: SpawnInfo): SWGObject {
		val spawnerType = SpawnerType.valueOf(spawn.spawnerType)
		val egg = ObjectCreator.createObjectFromTemplate(spawnerType.objectTemplate)
		egg.containerPermissions = AdminPermissions.getPermissions()
		if (spawn.patrolId.isEmpty() || spawn.patrolId == "0")
			egg.objectName = String.format("%s\nNPC: %s", spawn.id, spawn.npcId)
		else
			egg.objectName = String.format("%s\nNPC: %s\nG: %s", spawn.id, spawn.npcId, spawn.patrolId)
		egg.systemMove(getCell(spawn.id, spawn.cellId, spawn.buildingId), Location.builder().setTerrain(spawn.terrain).setPosition(spawn.x, spawn.y, spawn.z).setHeading(spawn.heading.toDouble()).build())
		ObjectCreatedIntent.broadcast(egg)
		return egg
	}
	
	private fun getCell(spawnId: String, cellId: Int, buildingTag: String): SWGObject? {
		if (buildingTag.isEmpty() || buildingTag.endsWith("_world"))
			return null
		val building = BuildingLookup.getBuildingByTag(buildingTag)
		if (building == null) {
			Log.w("Skipping spawner with ID %s - building_id %s didn't reference a BuildingObject!", spawnId, buildingTag)
			return null
		}
		
		val cellObject = building.getCellByNumber(cellId)
		if (cellObject == null) {
			Log.e("Spawner with ID %s - building %s didn't have cell ID %d!", spawnId, buildingTag, cellId)
		}
		return cellObject
	}
	
}
