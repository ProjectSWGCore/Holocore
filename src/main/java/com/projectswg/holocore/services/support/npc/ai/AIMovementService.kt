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
package com.projectswg.holocore.services.support.npc.ai

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.support.npc.ai.CompileNpcMovementIntent
import com.projectswg.holocore.intents.support.npc.ai.StartNpcMovementIntent
import com.projectswg.holocore.intents.support.npc.ai.StopNpcMovementIntent
import com.projectswg.holocore.resources.support.npc.ai.NavigationOffset
import com.projectswg.holocore.resources.support.npc.ai.NavigationPoint
import com.projectswg.holocore.resources.support.npc.ai.NavigationRouteType
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import com.projectswg.holocore.utilities.launchWithFixedRate
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.sin

class AIMovementService : Service() {
	
	private val routes = ConcurrentHashMap<AIObject, NavigationRoute>()
	private val coroutineScope = HolocoreCoroutine.childScope()
	
	override fun start(): Boolean {
		coroutineScope.launchWithFixedRate(1, TimeUnit.SECONDS) {
			routes.values.forEach { it.execute() } // TODO: put each route in its own coroutine
		}
		return true
	}
	
	override fun stop(): Boolean {
		coroutineScope.cancelAndWait()
		return super.stop()
	}
	
	@IntentHandler
	private fun handleStartNpcMovementIntent(snmi: StartNpcMovementIntent) {
		val obj = snmi.obj
		
		val route = NavigationPoint.from(obj.parent, obj.location, snmi.parent, snmi.destination, snmi.speed)
		if (route.isEmpty())
			routes.remove(obj)
		else
			routes[obj] = NavigationRoute(obj, route, NavigationRouteType.TERMINATE)
	}
	
	@IntentHandler
	private fun handleCompileNpcMovementIntent(snmi: CompileNpcMovementIntent) {
		val obj = snmi.obj
		val route = ArrayList<NavigationPoint>(snmi.points.size)
		val waypoints = snmi.points
		for ((index, point) in waypoints.withIndex()) {
			val next = waypoints.getOrNull(index+1) ?: waypoints.getOrNull(0)
			appendRoutePoint(route, offsetLocation(point, if (next == null) 0.0 else point.location.getHeadingTo(next.location), snmi.offset), snmi.speed)
		}
		
		if (route.isEmpty())
			routes.remove(obj)
		else
			routes[obj] = NavigationRoute(obj, route, snmi.type)
	}
	
	@IntentHandler
	private fun handleStopNpcMovementIntent(snmi: StopNpcMovementIntent) {
		routes.remove(snmi.obj)
	}
	
	@IntentHandler
	private fun handleCreatureKilledIntent(cki: CreatureKilledIntent) {
		val corpse = cki.corpse
		if (corpse is AIObject)
			routes.remove(corpse)
	}
	
	private fun appendRoutePoint(waypoints: MutableList<NavigationPoint>, waypoint: NavigationPoint, speed: Double) {
		val prev = if (waypoints.isEmpty()) null else waypoints[waypoints.size - 1]
		if (waypoint.isNoOperation) {
			waypoints.add(waypoint)
			return
		}
		if (prev == null) {
			waypoints.add(NavigationPoint.at(waypoint.parent, waypoint.location, speed))
		} else {
			if (prev.location.equals(waypoint.location) && prev.parent === waypoint.parent)
				return
			waypoints.addAll(NavigationPoint.from(prev.parent, prev.location, waypoint.parent, waypoint.location, speed))
		}
	}
	
	private class NavigationRoute(private val obj: AIObject, private val route: List<NavigationPoint>, private val type: NavigationRouteType) {
		
		private val index = AtomicInteger(0)
		
		fun execute() {
			var index = this.index.getAndIncrement()
			if (index >= route.size) {
				when (type) {
					NavigationRouteType.LOOP -> {
						this.index.set(0)
						index = 0
					}
					NavigationRouteType.TERMINATE -> {
						StopNpcMovementIntent(obj).broadcast()
						return
					}
				}
			}
			assert(index < route.size && index >= 0)
			
			route[index].move(obj)
		}
	}
	
	companion object {
		
		internal fun offsetLocation(point: NavigationPoint, heading: Double, offset: NavigationOffset?): NavigationPoint {
			return if (offset == null) point else NavigationPoint.at(point.parent, offsetLocation(point.location, heading, offset), point.speed)
		}
		
		internal fun offsetLocation(location: Location, heading: Double, offset: NavigationOffset): Location {
			val oX = offset.x
			val oZ = offset.z
			val cos = cos(Math.toRadians(heading)) // heading should be 0 - 360, with 0 representing north and 270 representing east
			val sin = sin(Math.toRadians(heading))
			val nX = oX * cos - oZ * sin
			val nZ = oZ * cos + oX * sin
			return Location.builder(location).setX(location.x + nX).setZ(location.z + nZ).build()
		}
	}
	
}
