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
package com.projectswg.holocore.resources.support.npc.spawn

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader.Faction
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcLoader.*
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcPatrolRouteLoader.PatrolRouteWaypoint
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcPatrolRouteLoader.PatrolType
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.*
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup
import me.joshlarson.jlcommon.log.Log
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors

class Spawner(spawn: StaticSpawnInfo, egg: SWGObject) {
	
	private val spawn: StaticSpawnInfo = Objects.requireNonNull(spawn, "spawn")
	private val npc: NpcInfo = Objects.requireNonNull(DataLoader.npcs().getNpc(spawn.npcId), "Invalid npc id: " + spawn.npcId)
	private val npcsInternal = CopyOnWriteArrayList<AIObject>()
	
	val location: Location = Location.builder().setTerrain(spawn.terrain).setPosition(spawn.x, spawn.y, spawn.z).setHeading(spawn.heading.toDouble()).build()
	val patrolRoute: List<ResolvedPatrolWaypoint>?
	val egg: SWGObject = Objects.requireNonNull(egg, "egg")
	val npcs: List<AIObject> = Collections.unmodifiableList(npcsInternal)
	
	/**
	 * Calculates a random number between `minRespawnDelay` and
	 * `maxRespawnDelay`
	 * @return a random number between `minRespawnDelay` and
	 * `maxRespawnDelay`
	 */
	val respawnDelay: Int
		get() = ThreadLocalRandom.current().nextInt(maxSpawnTime - minSpawnTime + 1) + minSpawnTime
	
	/**
	 * @return a random IFF template
	 */
	fun getRandomIffTemplate(): String = getRandom(iffs)
	fun getRandomPrimaryWeapon(): String = getRandom(primaryWeapons)
	fun getRandomSecondaryWeapon(): String = getRandom(secondaryWeapons)
	
	fun addNPC(obj: AIObject) {
		npcsInternal.add(obj)
	}
	
	fun removeNPC(obj: AIObject) {
		npcsInternal.remove(obj)
	}
	
	val id: String
		get() = spawn.id
	
	val spawnerType: String
		get() = spawn.spawnerType
	
	val npcId: String
		get() = spawn.npcId
	
	val buildingId: String
		get() = spawn.buildingId
	
	val mood: String
		get() = spawn.mood
	
	val behavior: AIBehavior
		get() = spawn.behavior
	
	val patrolId: String
		get() = spawn.patrolId
	
	val patrolFormation: PatrolFormation
		get() = spawn.patrolFormation
	
	val loiterRadius: Int
		get() = spawn.loiterRadius
	
	val minSpawnTime: Int
		get() = spawn.minSpawnTime
	
	val maxSpawnTime: Int
		get() = spawn.maxSpawnTime
	
	val amount: Int
		get() = spawn.amount
	
	val spawnerFlag: SpawnerFlag
		get() = spawn.spawnerFlag
	
	val difficulty: CreatureDifficulty
		get() = spawn.difficulty
	
	val minLevel: Int
		get() = spawn.minLevel
	
	val maxLevel: Int
		get() = spawn.maxLevel
	
	val name: String
		get() = npc.name
	
	val stfName: String
		get() = npc.stfName
	
	val iffs: List<String>
		get() = npc.iffs
	
	val faction: Faction?
		get() = npc.faction
	
	val isSpecForce: Boolean
		get() = npc.isSpecForce
	
	val attackSpeed: Double
		get() = npc.attackSpeed
	
	val movementSpeed: Double
		get() = npc.movementSpeed
	
	val scaleMin: Double
		get() = npc.scaleMin
	
	val scaleMax: Double
		get() = npc.scaleMax
	
	val hue: Int
		get() = npc.hue
	
	val primaryWeapons: List<String>
		get() = npc.primaryWeapons.stream().map { DataLoader.npcWeapons().getWeapons(it) }.filter { Objects.nonNull(it) }.flatMap { it.stream() }.collect(Collectors.toList())
	
	val secondaryWeapons: List<String>
		get() = npc.secondaryWeapons.stream().map { DataLoader.npcWeapons().getWeapons(it) }.filter { Objects.nonNull(it) }.flatMap { it.stream() }.collect(Collectors.toList())
	
	val aggressiveRadius: Int
		get() = npc.aggressiveRadius
	
	val assistRadius: Int
		get() = npc.assistRadius
	
	val isDeathblow: Boolean
		get() = npc.isDeathblow
	
	val lootTable1: String
		get() = npc.lootTable1
	
	val lootTable2: String
		get() = npc.lootTable2
	
	val lootTable3: String
		get() = npc.lootTable3
	
	val lootTable1Chance: Int
		get() = npc.lootTable1Chance
	
	val lootTable2Chance: Int
		get() = npc.lootTable2Chance
	
	val lootTable3Chance: Int
		get() = npc.lootTable3Chance
	
	val humanoidInfo: HumanoidNpcInfo
		get() = npc.humanoidInfo
	
	val droidInfo: DroidNpcInfo
		get() = npc.droidInfo
	
	val creatureInfo: CreatureNpcInfo
		get() = npc.creatureInfo
	
	init {
		if (spawn.patrolId.isEmpty() || spawn.patrolId == "0") { // TODO: Replace the latter with empty string
			this.patrolRoute = null
		} else {
			val waypoints = Objects.requireNonNull(DataLoader.npcPatrolRoutes().getPatrolRoute(spawn.patrolId), "Invalid patrol route: " + spawn.patrolId)
			this.patrolRoute = waypoints.stream().map { ResolvedPatrolWaypoint(it) }.collect(Collectors.toList())
		}
	}
	
	private fun <T> getRandom(list: List<T>): T {
		return list[ThreadLocalRandom.current().nextInt(list.size)]
	}
	
	class ResolvedPatrolWaypoint(private val waypoint: PatrolRouteWaypoint) {
		val parent: SWGObject?
		val location: Location
		
		val delay: Double
			get() = waypoint.delay
		
		val patrolType: PatrolType
			get() = waypoint.patrolType
		
		val groupId: String
			get() = waypoint.groupId
		
		val patrolId: String
			get() = waypoint.patrolId
		
		init {
			this.parent = getPatrolWaypointParent(waypoint)
			this.location = getPatrolWaypointLocation(waypoint)
		}
		
		private fun getPatrolWaypointLocation(waypoint: PatrolRouteWaypoint): Location {
			return Location.builder()
					.setTerrain(waypoint.terrain)
					.setX(waypoint.x)
					.setY(waypoint.y)
					.setZ(waypoint.z).build()
		}
		
		private fun getPatrolWaypointParent(waypoint: PatrolRouteWaypoint): SWGObject? {
			if (waypoint.buildingId.isEmpty() || waypoint.buildingId.endsWith("_world"))
				return null
			
			val building = BuildingLookup.getBuildingByTag(waypoint.buildingId)
			if (building == null) {
				Log.w("PatrolRouteWaypoint: Invalid building id for patrol id: %d and group id: %d", waypoint.patrolId, waypoint.groupId)
				return null
			}
			
			val cell = building.getCellByNumber(waypoint.cellId)
			if (cell == null) {
				Log.w("PatrolRouteWaypoint: Invalid cell [%d] for building: %s, patrol id: %d and group id: %d", waypoint.cellId, waypoint.buildingId, waypoint.patrolId, waypoint.groupId)
				return null
			}
			
			return cell
		}
		
	}
	
}
