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
package com.projectswg.holocore.resources.support.npc.ai

import com.projectswg.holocore.intents.support.npc.ai.CompileNpcMovementIntent
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcPatrolRouteLoader.PatrolType
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.npc.spawn.Spawner.ResolvedPatrolWaypoint
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode
import java.util.*
import kotlin.math.ceil

/**
 * AI object that patrols the specified route
 */
class NpcPatrolMode(obj: AIObject, waypoints: MutableList<ResolvedPatrolWaypoint>) : NpcMode(obj) {
	
	private val waypoints: MutableList<NavigationPoint>
	
	init {
		var waypointBuilder = waypoints
		waypointBuilder = ArrayList(waypointBuilder)
		
		if (waypointBuilder.isNotEmpty() && waypointBuilder[0].patrolType == PatrolType.FLIP) {
			waypointBuilder.addAll(waypointBuilder.reversed())
		} else if (waypointBuilder.isNotEmpty() && waypointBuilder[0].patrolType == PatrolType.LOOP) {
			waypointBuilder.add(waypointBuilder[0])
		}
		
		this.waypoints = ArrayList(waypointBuilder.size)
		for (waypoint in waypointBuilder) {
			val point = NavigationPoint.at(waypoint.parent, waypoint.location, walkSpeed)
			this.waypoints.add(point)
			this.waypoints.addAll(NavigationPoint.nop(point, waypoint.delay.toInt()))
		}
	}
	
	override fun onModeStart() {
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
				ScheduleNpcModeIntent.broadcast(ai, NpcNavigateMode(ai, waypoints[index]))
				return
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
		val spawner = spawner
		val spacing = 3.0
		val position = spawner.npcs.indexOf(ai)
		assert(position != -1)
		
		when (spawner.patrolFormation) {
			NpcStaticSpawnLoader.PatrolFormation.NONE -> CompileNpcMovementIntent.broadcast(ai, compiledWaypoints, NavigationRouteType.LOOP, walkSpeed, null)
			NpcStaticSpawnLoader.PatrolFormation.COLUMN -> {
				val x = if (position % 2 == 0) 0.0 else spacing
				val z = -(spacing * ceil((position - 1) / 2.0))
				CompileNpcMovementIntent.broadcast(ai, compiledWaypoints, NavigationRouteType.LOOP, walkSpeed, NavigationOffset(x, z))
			}
			NpcStaticSpawnLoader.PatrolFormation.WEDGE -> {
				val x = spacing * ceil(position / 2.0)
				val z = -x
				CompileNpcMovementIntent.broadcast(ai, compiledWaypoints, NavigationRouteType.LOOP, walkSpeed, NavigationOffset(if (position % 2 == 0) -x else x, z))
			}
			NpcStaticSpawnLoader.PatrolFormation.LINE -> {
				val x = spacing * ceil(position / 2.0)
				CompileNpcMovementIntent.broadcast(ai, compiledWaypoints, NavigationRouteType.LOOP, walkSpeed, NavigationOffset(if (position % 2 == 0) -x else x, 0.0))
			}
			NpcStaticSpawnLoader.PatrolFormation.BOX -> {
				val x = when (position) {
					0, 1, 2 -> position * 3.0
					3       -> 0.0
					4       -> 6.0
					else    -> (position - 5) * 3.0
				}
				val z = when (position) {
					0, 1, 2 -> 0.0 // front of the box
					3, 4    -> 3.0 // sides of the box
					else    -> 6.0 // back of the box
				}
				CompileNpcMovementIntent.broadcast(ai, compiledWaypoints, NavigationRouteType.LOOP, walkSpeed, NavigationOffset(x, z))
			}
		}
	}
	
	override fun act() {
		
	}
	
}
