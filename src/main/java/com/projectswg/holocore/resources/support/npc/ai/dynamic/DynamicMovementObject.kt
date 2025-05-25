/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.resources.support.npc.ai.dynamic

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.permissions.AdminPermissions
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class DynamicMovementObject(var location: Location, val name: String, val baseSpeed: Double = 0.0) {

	private val random = Random(System.currentTimeMillis())
	var heading = random.nextDouble() * 2 * Math.PI
	private val groupMarker = ObjectCreator.createObjectFromTemplate("object/path_waypoint/shared_path_waypoint_droid.iff")
	private val npcs = ArrayList<AIObject>()
	private var lastUpdate = System.nanoTime()
	
	init {
		groupMarker.location = location
		groupMarker.objectName = name
		groupMarker.containerPermissions = AdminPermissions.getPermissions()
	}
	
	fun launch() {
		ObjectCreatedIntent(groupMarker).broadcast()
		val simpleSpawnInfo = SimpleSpawnInfo.builder()
			.withNpcId("humanoid_tusken_soldier")
			.withDifficulty(CreatureDifficulty.NORMAL)
			.withSpawnerType(SpawnerType.WAYPOINT_AUTO_SPAWN)
			.withMinLevel(10)
			.withMaxLevel(30)
			.withSpawnerFlag(NpcStaticSpawnLoader.SpawnerFlag.AGGRESSIVE)
			.withBehavior(AIBehavior.IDLE)
			.withLocation(location)
		val bossSpawner = Spawner(simpleSpawnInfo.withDifficulty(CreatureDifficulty.BOSS).build(), groupMarker)
		val eliteSpawner = Spawner(simpleSpawnInfo.withDifficulty(CreatureDifficulty.ELITE).build(), groupMarker)
		val normalSpawner = Spawner(simpleSpawnInfo.withDifficulty(CreatureDifficulty.NORMAL).build(), groupMarker)
		if (random.nextDouble() < 0.25)
			npcs.add(NPCCreator.createSingleNpc(bossSpawner))
		npcs.add(NPCCreator.createSingleNpc(eliteSpawner))
		repeat(15) {
			npcs.add(NPCCreator.createSingleNpc(normalSpawner))
		}
	}
	
	fun destroy() {
		DestroyObjectIntent(groupMarker).broadcast()
	}
	
	fun act() {
		assert(!isOutOfBounds(location))
		assert(heading in 0.0..Math.TAU)
		val currentTime = System.nanoTime()
		val npcSpeed = if (baseSpeed != 0.0) baseSpeed else npcs[0].walkSpeed.toDouble()
		val elapsedTime = (currentTime - lastUpdate) / 1E9
		val distance = elapsedTime * npcSpeed // TODO: scale based on terrain, somewhat
		lastUpdate = currentTime
		moveNextPosition(distance)
		groupMarker.moveToLocation(location)
		npcs.forEachIndexed { i, it ->
			// Spiral shape for now
			val radius = 1 + i * 0.5
			val angle = i * (Math.PI * 0.61)
			val newLocationBuilder = Location.builder(location)
				.setX(location.x + radius * cos(angle))
				.setZ(location.z + radius * sin(angle))
			newLocationBuilder.setY(ServerData.terrains.getHeight(newLocationBuilder))
			val newLocation = newLocationBuilder.build()
			val speed = min(30.0, max(1.0, it.worldLocation.distanceTo(newLocation) / elapsedTime))
			it.moveTo(null, newLocationBuilder.build(), speed)
		}
	}
	
	private fun moveNextPosition(distance: Double) {
		val firstProposed = calculateNextPosition(heading, distance)
		if (isValidNextPosition(firstProposed)) {
			location = firstProposed
			return
		}

		val newHeading = heading + Math.PI * (1 + random.nextDouble() - 0.5)
		val secondProposed = calculateNextPosition(newHeading, distance)
		if (isValidNextPosition(secondProposed)) {
			location = secondProposed
			heading = if (newHeading >= Math.TAU) newHeading - Math.TAU else newHeading
			return
		}
		
		// Brute Force Escape
		val randomRotationFromNorth = random.nextDouble() * Math.TAU
		for (clockwiseRotation in 0..35) {
			val bruteForceHeading = (clockwiseRotation * 10) * Math.PI / 180.0 + randomRotationFromNorth
			val proposed = calculateNextPosition(bruteForceHeading, distance)
			if (isValidNextPosition(proposed)) {
				location = proposed
				heading = if (bruteForceHeading >= Math.TAU) bruteForceHeading - Math.TAU else bruteForceHeading
				return
			}
		}
		
		// TODO: destroy this object, we got stuck
		location = firstProposed
		assert(false)
	}
	
	private fun isValidNextPosition(proposed: Location): Boolean {
		return !DynamicMovementProcessor.isIntersectingProtectedZone(location, proposed) && !isOutOfBounds(proposed)
	}
	
	private fun isOutOfBounds(location: Location): Boolean {
		return location.x < -7000 || location.x > 7000 || location.z < -7000 || location.z > 7000
	}
	
	private fun calculateNextPosition(heading: Double, distance: Double): Location {
		val nextLocationBuilder = Location.builder(location)
			.setX(location.x + distance * cos(heading))
			.setZ(location.z + distance * sin(heading))
			.setHeading(heading)
		nextLocationBuilder.setY(ServerData.terrains.getHeight(nextLocationBuilder))
		return nextLocationBuilder.build()
	}
	
}