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
import com.projectswg.common.data.location.Point2f
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.sqrt

object DynamicMovementProcessor {
	
	fun createSpawnLocation(terrain: Terrain): Location? {
		val randomizer = ThreadLocalRandom.current()
		repeat(100) {
			val locationBuilder = Location.builder()
				.setTerrain(terrain)
				.setX(randomizer.nextDouble() * 7000 * 2 - 7000)
				.setZ(randomizer.nextDouble() * 7000 * 2 - 7000)
			locationBuilder.setY(ServerData.terrains.getHeight(locationBuilder))
			val location = locationBuilder.build()
			if (isGoodSpawnLocation(location))
				return location
		}
		return null
	}
	
	fun isGoodSpawnLocation(point: Location): Boolean {
		val zones = ServerData.noSpawnZones.getNoSpawnZoneInfos(point.terrain)
		val point2f = Point2f(point.x.toFloat(), point.z.toFloat())
		for (zone in zones) {
			val zoneCenter = Point2f(zone.x.toFloat(), zone.z.toFloat())
			val distance = sqrt(point2f.squaredDistanceTo(zoneCenter))
			if (distance < zone.radius.toFloat()) {
				return false
			}
		}
		return true
	}
	
	fun isIntersectingProtectedZone(start: Location, end: Location): Boolean {
		assert(start.terrain == end.terrain)
		val zones = ServerData.noSpawnZones.getNoSpawnZoneInfos(start.terrain)
		val startPoint = Point2f(start.x.toFloat(), start.z.toFloat())
		val endPoint = Point2f(end.x.toFloat(), end.z.toFloat())
		
		for (zone in zones) {
			val zoneCenter = Point2f(zone.x.toFloat(), zone.z.toFloat())
			val closestPoint = startPoint.getClosestPointOnLineSegment(endPoint, zoneCenter)
			val distance = sqrt(closestPoint.squaredDistanceTo(zoneCenter))
			if (distance < zone.radius.toFloat()) {
				return true
			}
		}
		return false
	}
	
}