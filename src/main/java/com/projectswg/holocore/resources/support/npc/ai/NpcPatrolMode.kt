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
package com.projectswg.holocore.resources.support.npc.ai

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcPatrolRouteLoader.PatrolType
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.npc.spawn.Spawner.ResolvedPatrolWaypoint
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode
import kotlinx.coroutines.CancellationException
import kotlin.math.ceil

/**
 * AI object that patrols the specified route
 */
class NpcPatrolMode(obj: AIObject, waypoints: List<ResolvedPatrolWaypoint>) : NpcMode(obj) {
	
	private val waypoints: MutableList<NavigationPoint>
	
	init {
		var waypointBuilder = waypoints
		waypointBuilder = ArrayList(waypointBuilder)
		
		if (waypointBuilder.isNotEmpty() && waypointBuilder[0].patrolType == PatrolType.FLIP) {
			waypointBuilder.addAll(waypointBuilder.reversed())
		} else if (waypointBuilder.isNotEmpty() && waypointBuilder[0].patrolType == PatrolType.LOOP) {
			waypointBuilder.add(waypointBuilder[0])
		}
		
		this.waypoints = ArrayList<NavigationPoint>(128)
		for (i in 1 until waypointBuilder.size) {
			val source = waypointBuilder[i - 1]
			val destination = waypointBuilder[i]
			this.waypoints.addAll(NavigationPoint.from(source.parent, source.location, destination.parent, destination.location, walkSpeed))
			if (destination.delay > 0)
				this.waypoints.addAll(NavigationPoint.nop(this.waypoints[this.waypoints.size - 1], destination.delay.toInt() - 1))
		}
		this.waypoints.addAll(NavigationPoint.from(waypointBuilder[waypointBuilder.size - 1].parent, waypointBuilder[waypointBuilder.size - 1].location, waypointBuilder[0].parent, waypointBuilder[0].location, walkSpeed))
	}
	
	override suspend fun onModeStart() {
		val route = calculateRouteOffset(calculateInitialRoutePoints())
		
		ai.moveVia(route, loop = true)
	}
	
	override suspend fun onModeLoop() {
		throw CancellationException() // No loop necessary
	}
	
	private fun calculateInitialRoutePoints(): List<NavigationPoint> {
		val compiledWaypoints: MutableList<NavigationPoint>
		if (waypoints.isNotEmpty()) {
			var index = 0
			var closestDistance = waypoints[0].distanceTo(ai)
			for (i in 1 until waypoints.size) {
				if (waypoints[i].isNoOperation)
					continue

				val distance = waypoints[i].distanceTo(ai)
				if (distance < closestDistance) {
					closestDistance = distance
					index = i
				}
			}
			if (closestDistance >= 2) {
				ai.activeMode = NpcNavigateMode(ai, waypoints[index])
				throw CancellationException()
			}
			compiledWaypoints = ArrayList(waypoints.size)
			for (i in index until waypoints.size) {
				compiledWaypoints.add(waypoints[i])
			}
			for (i in 0 until index) {
				compiledWaypoints.add(waypoints[i])
			}
		} else {
			compiledWaypoints = waypoints
		}
		return compiledWaypoints
	}
	
	private fun calculateRouteOffset(compiledWaypoints: List<NavigationPoint>): List<NavigationPoint> {
		val spawner = spawner ?: throw CancellationException()
		val spacing = 3.0
		val position = spawner.npcs.indexOf(ai)
		var offsetX = 0.0
		var offsetZ = 0.0
		assert(position != -1)
		
		when (spawner.patrolFormation) {
			NpcStaticSpawnLoader.PatrolFormation.NONE -> {}
			NpcStaticSpawnLoader.PatrolFormation.COLUMN -> {
				offsetX = if (position % 2 == 0) 0.0 else spacing
				offsetZ = -(spacing * ceil((position - 1) / 2.0))
			}
			NpcStaticSpawnLoader.PatrolFormation.WEDGE -> {
				offsetX = spacing * ceil(position / 2.0) * (if (position % 2 == 0) -1 else 1)
				offsetZ = -offsetX
			}
			NpcStaticSpawnLoader.PatrolFormation.LINE -> {
				offsetX = spacing * ceil(position / 2.0) * (if (position % 2 == 0) -1 else 1)
			}
			NpcStaticSpawnLoader.PatrolFormation.BOX -> {
				offsetX = when (position) {
					0, 1, 2 -> position * 3.0
					3       -> 0.0
					4       -> 6.0
					else    -> (position - 5) * 3.0
				}
				offsetZ = when (position) {
					0, 1, 2 -> 0.0 // front of the box
					3, 4    -> 3.0 // sides of the box
					else    -> 6.0 // back of the box
				}
			}
		}
		
		val offsetWaypoints = ArrayList<NavigationPoint>(compiledWaypoints.size)
		for (wp in compiledWaypoints)
			offsetWaypoints.add(NavigationPoint(wp.parent, Location.builder(wp.location).translatePosition(offsetX, 0.0, offsetZ).build(), wp.speed))
		return offsetWaypoints
	}
	
}
