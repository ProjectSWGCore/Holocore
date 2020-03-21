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
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.sin

class AIMovementService : Service() {
	
	private val routes = ConcurrentHashMap<AIObject, NavigationRoute>()
	private val movementThreads = ScheduledThreadPool(Runtime.getRuntime().availableProcessors(), "ai-movement-service-%d")
	
	override fun start(): Boolean {
		movementThreads.start()
		movementThreads.executeWithFixedRate(1000, 1000) { this.executeRoutes() }
		return true
	}
	
	override fun stop(): Boolean {
		movementThreads.stop()
		return movementThreads.awaitTermination(1000)
	}
	
	@IntentHandler
	private fun handleStartNpcMovementIntent(snmi: StartNpcMovementIntent) {
		val obj = snmi.getObject()
		
		val route = NavigationPoint.from(obj.parent, obj.location, snmi.parent, snmi.destination, snmi.speed)
		if (route.isEmpty())
			routes.remove(obj)
		else
			routes[obj] = NavigationRoute(obj, route, NavigationRouteType.TERMINATE)
	}
	
	@IntentHandler
	private fun handleCompileNpcMovementIntent(snmi: CompileNpcMovementIntent) {
		val obj = snmi.getObject()
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
		routes.remove(snmi.getObject())
	}
	
	@IntentHandler
	private fun handleCreatureKilledIntent(cki: CreatureKilledIntent) {
		val corpse = cki.corpse
		if (corpse is AIObject)
			routes.remove(corpse)
	}
	
	private fun executeRoutes() {
		routes.values.forEach { it.execute() }
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
						StopNpcMovementIntent.broadcast(obj)
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
