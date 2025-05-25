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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.swgiff.parsers.SWGParser
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcPatrolRouteLoader
import com.projectswg.holocore.resources.support.npc.ai.NavigationPoint
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.services.support.objects.ObjectStorageService
import com.projectswg.holocore.test.runners.TestRunnerNoIntents
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class NpcPatrolRouteLoaderTest : TestRunnerNoIntents() {

	@Test
	fun `test patrol route waypoints`() {
		fun checkRouteLeg(sourceWaypoint: Spawner.ResolvedPatrolWaypoint, destinationWaypoint: Spawner.ResolvedPatrolWaypoint) {
			val route = NavigationPoint.from(sourceWaypoint.parent, sourceWaypoint.location, destinationWaypoint.parent, destinationWaypoint.location, 1.0)
			assert(route.size < 500) { "distance between waypoints is too large (${route.size})" }
			assert(sourceWaypoint.location.terrain == destinationWaypoint.location.terrain) { "terrain mismatch along route" }
		}

		val hasError = AtomicBoolean(false)
		ServerData.npcPatrolRoutes.forEach { route ->
			try {
				val resolvedRoute = route.map { Spawner.ResolvedPatrolWaypoint(it) }
				assert(route.isNotEmpty()) { "route is empty" }
				for (i in 1 until route.size) {
					checkRouteLeg(resolvedRoute[i - 1], resolvedRoute[i])
				}
				if (route[0].patrolType == NpcPatrolRouteLoader.PatrolType.LOOP) checkRouteLeg(resolvedRoute[route.size - 1], resolvedRoute[0])
			} catch (e: AssertionError) {
				System.err.println("Patrol group '${route[0].groupId}' error: ${e.message}")
				hasError.set(true)
			}
		}
		Assertions.assertFalse(hasError.get())
	}

	@Test
	fun `test NPC to patrol route start`() {
		val hasError = AtomicBoolean(false)
		ServerData.npcStaticSpawns.spawns.parallelStream().forEach { spawn ->
			if (spawn.patrolId.isEmpty() || spawn.patrolId == "0") return@forEach
			val spawnerLocation = Location.builder().setTerrain(spawn.terrain).setX(spawn.x).setY(spawn.y).setZ(spawn.z).build()
			val route = ServerData.npcPatrolRoutes[spawn.patrolId]
			val routeLocation = Location.builder().setTerrain(route[0].terrain).setX(route[0].x).setY(route[0].y).setZ(route[0].z).build()
			val distanceToRoute = spawnerLocation.distanceTo(routeLocation)
			try {
				assert(spawn.buildingId == route[0].buildingId) { "NPC not in same building as route" }
				assert(spawn.cellId == route[0].cellId) { "NPC not in same cell as route" }
				assert(distanceToRoute < 500) { "Spawner distance to route too large ($distanceToRoute)" }
				assert(spawnerLocation.terrain == routeLocation.terrain) { "terrain mismatch along route" }
			} catch (e: AssertionError) {
				System.err.println("Patrol spawner '${spawn.npcId}' with route '${spawn.patrolId}' error: ${e.message}")
				hasError.set(true)
			}
		}
		Assertions.assertFalse(hasError.get())
	}

	companion object {

		private var objectStorageService = ObjectStorageService()

		@BeforeAll
		@JvmStatic
		fun setup() {
			SWGParser.setBasePath("serverdata")
			objectStorageService.initialize()
		}

		@AfterAll
		@JvmStatic
		fun tearDown() {
			objectStorageService.terminate()
		}
	}

}