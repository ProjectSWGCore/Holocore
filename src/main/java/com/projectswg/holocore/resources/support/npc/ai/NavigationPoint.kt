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
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.terrains
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal
import java.util.*
import kotlin.math.atan2
import kotlin.math.floor

class NavigationPoint(val parent: SWGObject?, val location: Location, val speed: Double) {
	private val hash = Objects.hash(parent, location)

	fun move(obj: SWGObject) {
		if (isNoOperation) return
		if (parent == null) obj.broadcast(MoveObjectIntent(obj, location, speed))
		else obj.broadcast(MoveObjectIntent(obj, parent, location, speed))
	}

	val isNoOperation: Boolean
		get() = speed == 0.0

	fun distanceTo(otherParent: SWGObject?, otherLocation: Location?): Double {
		var otherLocation = otherLocation
		var myLocation = location
		if (parent != null) myLocation = Location.builder(myLocation).translateLocation(parent.worldLocation).build()
		if (otherParent != null) otherLocation = Location.builder(otherLocation).translateLocation(otherParent.worldLocation).build()
		return myLocation.distanceTo(otherLocation)
	}

	fun distanceTo(point: NavigationPoint): Double {
		return distanceTo(point.parent, point.location)
	}

	fun distanceTo(obj: SWGObject): Double {
		return distanceTo(obj.parent, obj.location)
	}

	override fun equals(other: Any?): Boolean {
		if (other !is NavigationPoint) return false
		return hash == other.hash && parent === other.parent && location == other.location
	}

	override fun hashCode(): Int {
		return hash
	}

	override fun toString(): String {
		return String.format("NavigationPoint[%s @ %s]", parent, location.position)
	}

	private class NavigationRouteNode(private val route: List<Portal>, val node: CellObject?, start: Location, destination: Location?) : Comparable<NavigationRouteNode> {
		private val cost: Double

		init {
			var start = start
			var cost = 0.0
			for (portal in route) {
				val portalLocation = buildWorldPortalLocation(portal)
				cost += portalLocation.distanceTo(start)
				start = portalLocation
			}
			this.cost = cost + start.distanceTo(destination)
		}

		fun getRoute(): List<Portal> {
			return Collections.unmodifiableList(route)
		}

		override fun equals(other: Any?): Boolean {
			return other is NavigationRouteNode && other.node === node
		}

		override fun hashCode(): Int {
			return node.hashCode()
		}

		override fun compareTo(other: NavigationRouteNode): Int {
			return cost.compareTo(other.cost)
		}
	}

	companion object {
		fun nop(prev: NavigationPoint, intervals: Int): List<NavigationPoint> {
			val nop: MutableList<NavigationPoint> = ArrayList()
			for (i in 0 until intervals) nop.add(NavigationPoint(prev.parent, prev.location, 0.0))
			return nop
		}

		fun at(parent: SWGObject?, location: Location, speed: Double): NavigationPoint {
			return NavigationPoint(parent, location, speed)
		}

		fun from(sourceParent: SWGObject?, source: Location, destinationParent: SWGObject?, destination: Location, speed: Double): List<NavigationPoint> {
			assert(sourceParent == null || sourceParent is CellObject) { "invalid source parent" }
			assert(destinationParent == null || destinationParent is CellObject) { "invalid destination parent" }
			assert(speed > 0) { "speed must be greater than zero, was $speed" }

			if (sourceParent == destinationParent) return from(sourceParent, source, destination, speed)

			var source = source
			val route = getBuildingRoute(sourceParent as CellObject?, destinationParent as CellObject?, source, destination) ?: return ArrayList()
			val points = createIntraBuildingRoute(route, sourceParent, source, speed)
			if (route.isNotEmpty()) source = if (destinationParent == null) buildWorldPortalLocation(route[route.size - 1]) else buildPortalLocation(route[route.size - 1])
			points.addAll(from(destinationParent, source, destination, speed))
			return points
		}

		/**
		 * Returns a list of locations to traverse on the path to the specified destination, at the specified speed
		 *
		 * @param parent      the parent for each point
		 * @param source      the source location
		 * @param destination the destination location
		 * @param speed       the speed to travel at
		 * @return a queue of locations to travel
		 */
		fun from(parent: SWGObject?, source: Location, destination: Location, speed: Double): List<NavigationPoint> {
			var speed = speed
			speed = floor(speed)
			val totalDistance = source.distanceTo(destination)
			val path: MutableList<NavigationPoint> = ArrayList()

			assert(speed > 0) { "speed must be greater than zero, was $speed" }
			assert(totalDistance < 5_000) { "distance between waypoints is too large ($totalDistance)" }

			var currentDistance = speed
			while (currentDistance < totalDistance) {
				path.add(interpolate(parent, source, destination, speed, currentDistance / totalDistance))
				currentDistance += speed
				assert(path.size < 10_000) { "path length growing too large" }
			}
			path.add(interpolate(parent, source, destination, speed, 1.0))
			return path
		}

		private fun interpolate(parent: SWGObject?, l1: Location, l2: Location, speed: Double, percentage: Double): NavigationPoint {
			val heading = Math.toDegrees(atan2(l2.x - l1.x, l2.z - l1.z))
			if (percentage <= 0) return NavigationPoint(
				parent, Location.builder(l1).setY(if (parent == null) terrains().getHeight(l1) else l1.y).setHeading(heading).build(), speed
			)
			if (percentage >= 1) return NavigationPoint(
				parent, Location.builder(l2).setY(if (parent == null) terrains().getHeight(l2) else l2.y).setHeading(heading).build(), speed
			)

			val interpX = l1.x + (l2.x - l1.x) * percentage
			val interpY = l1.y + (l2.y - l1.y) * percentage
			val interpZ = l1.z + (l2.z - l1.z) * percentage
			return NavigationPoint(
				parent, Location.builder().setTerrain(l1.terrain).setX(interpX).setY(if (parent == null) terrains().getHeight(l1.terrain, interpX, interpZ) else interpY).setZ(interpZ).setHeading(heading).build(), speed
			)
		}

		private fun createIntraBuildingRoute(route: List<Portal>, from: CellObject?, start: Location, speed: Double): MutableList<NavigationPoint> {
			var from = from
			var start = start
			val points: MutableList<NavigationPoint> = ArrayList()
			for (portal in route) {
				if (from == null) points.addAll(from(null, start, buildWorldPortalLocation(portal), speed))
				else points.addAll(from(from, start, buildPortalLocation(portal), speed))
				from = portal.getOtherCell(from)
				start = buildPortalLocation(portal)
			}
			return points
		}

		private fun getBuildingRoute(from: CellObject?, to: CellObject?, start: Location, destination: Location): List<Portal>? {
			if (from === to) return listOf()
			if (from != null) {
				for (fromPortal in from.getPortals()) {
					if (fromPortal.getOtherCell(from) === to) return listOf(fromPortal)
				}
			}

			val nodes = PriorityQueue(getNearbyPortals(NavigationRouteNode(ArrayList(), from, start, destination), to, start, destination))
			while (!nodes.isEmpty()) {
				val node = checkNotNull(nodes.poll()) { "loop precondition" }
				if (node.node === to) return node.getRoute()

				nodes.addAll(getNearbyPortals(node, to, start, destination))
			}
			return null
		}

		private fun getNearbyPortals(node: NavigationRouteNode, to: CellObject?, start: Location, destination: Location): List<NavigationRouteNode> {
			val nearby: MutableList<NavigationRouteNode> = ArrayList()
			if (node.node == null) {
				val building = checkNotNull(to!!.parent as BuildingObject?)
				for (portal in building.getPortals()) {
					if (node.getRoute().contains(portal)) continue
					if (portal.cell1 != null && portal.cell2 != null) continue

					nearby.add(NavigationRouteNode(appendToCopy(node.getRoute(), portal), portal.getOtherCell(null), start, destination))
				}
			} else {
				for (portal in node.node.getPortals()) {
					if (node.getRoute().contains(portal)) continue
					nearby.add(NavigationRouteNode(appendToCopy(node.getRoute(), portal), portal.getOtherCell(node.node), start, destination))
				}
			}
			return nearby
		}

		private fun <T> appendToCopy(original: List<T>, newElement: T): List<T> {
			val replacement: MutableList<T> = ArrayList(original)
			replacement.add(newElement)
			return replacement
		}

		private fun buildWorldPortalLocation(portal: Portal): Location {
			val building = portal.cell1!!.parent
			assert(building is BuildingObject) { "cell parent wasn't a building" }
			return Location.builder(buildPortalLocation(portal)).translateLocation(building!!.location).build()
		}

		private fun buildPortalLocation(portal: Portal): Location {
			return Location.builder().setX(average(portal.frame1.x, portal.frame2.x)).setY(average(portal.frame1.y, portal.frame2.y)).setZ(average(portal.frame1.z, portal.frame2.z)).setTerrain(portal.cell1!!.terrain).build()
		}

		private fun average(x: Double, y: Double): Double {
			return (x + y) / 2
		}
	}
}
