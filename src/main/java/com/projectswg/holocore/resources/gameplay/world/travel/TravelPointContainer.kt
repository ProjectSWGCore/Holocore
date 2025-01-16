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
package com.projectswg.holocore.resources.gameplay.world.travel

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import java.util.concurrent.ConcurrentHashMap

class TravelPointContainer {
	private val shuttlePoints: MutableMap<Terrain, MutableList<TravelPoint>> = ConcurrentHashMap()
	private val starportPoints: MutableMap<Terrain, MutableList<TravelPoint>> = ConcurrentHashMap()

	fun addTravelPoint(point: TravelPoint) {
		val terrain = point.location.terrain
		getTravelPoints(terrain, point.isStarport).add(point)
	}

	fun getPointsForTerrain(nearest: TravelPoint?, to: Terrain): MutableList<TravelPoint> {
		val points: MutableList<TravelPoint> = ArrayList()
		if (nearest!!.location.terrain == to) {
			points.addAll(getTravelPoints(to, false))
			points.addAll(getTravelPoints(to, true))
		} else if (nearest.isStarport) {
			points.addAll(getTravelPoints(to, true))
		}
		return points
	}

	fun getNearestPoint(l: Location): TravelPoint? {
		var nearest: TravelPoint? = null
		var dist = Double.MAX_VALUE
		// Checks shuttleports
		for (tp in getTravelPoints(l.terrain, false)) {
			val tpDistance = tp.location.flatDistanceTo(l)
			if (tpDistance < dist && tpDistance < 25) {
				nearest = tp
				dist = tpDistance
			}
		}
		// Checks starports
		for (tp in getTravelPoints(l.terrain, true)) {
			val tpDistance = tp.location.flatDistanceTo(l)
			if (tpDistance < dist && tpDistance < 75) {
				nearest = tp
				dist = tpDistance
			}
		}
		return nearest
	}

	fun getDestination(t: Terrain, destination: String): TravelPoint? {
		// Check shuttleports
		for (tp in getTravelPoints(t, false)) {
			if (tp.name == destination) return tp
		}
		// Check starports
		for (tp in getTravelPoints(t, true)) {
			if (tp.name == destination) return tp
		}
		return null
	}

	private fun getTravelPoints(terrain: Terrain, starport: Boolean): MutableList<TravelPoint> {
		val travelPoints: MutableMap<Terrain, MutableList<TravelPoint>> = if (starport) starportPoints else shuttlePoints
		val points = travelPoints.computeIfAbsent(terrain) { _: Terrain? -> ArrayList() }
		return points
	}
}
