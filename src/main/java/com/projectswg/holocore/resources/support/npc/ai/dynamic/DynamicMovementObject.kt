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
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin

class DynamicMovementObject(var location: Location) {
	
	var heading = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI
	
	fun move() {
		assert(!isOutOfBounds(location))
		assert(heading in 0.0..Math.TAU)
		moveNextPosition()
	}
	
	private fun moveNextPosition() {
		val firstProposed = calculateNextPosition(heading)
		if (isValidNextPosition(firstProposed)) {
			location = firstProposed
			return
		}

		val newHeading = heading + Math.PI * (1 + ThreadLocalRandom.current().nextDouble() - 0.5)
		val secondProposed = calculateNextPosition(newHeading)
		if (isValidNextPosition(secondProposed)) {
			location = secondProposed
			heading = if (newHeading >= Math.TAU) newHeading - Math.TAU else newHeading
			return
		}
		
		// Brute Force Escape
		val randomRotationFromNorth = ThreadLocalRandom.current().nextDouble() * Math.TAU
		for (clockwiseRotation in 0..35) {
			val bruteForceHeading = (clockwiseRotation * 10) * Math.PI / 180.0 + randomRotationFromNorth
			val proposed = calculateNextPosition(bruteForceHeading)
			if (isValidNextPosition(proposed)) {
				location = proposed
				heading = if (bruteForceHeading >= Math.TAU) bruteForceHeading - Math.TAU else bruteForceHeading
				return
			}
		}
		
		// TODO: destroy this object, we got stuck
		assert(false)
	}
	
	private fun isValidNextPosition(proposed: Location): Boolean {
		return !DynamicMovementProcessor.isIntersectingProtectedZone(location, proposed) && !isOutOfBounds(proposed)
	}
	
	private fun isOutOfBounds(location: Location): Boolean {
		return location.x < -7000 || location.x > 7000 || location.z < -7000 || location.z > 7000
	}
	
	private fun calculateNextPosition(heading: Double): Location {
		val nextLocationBuilder = Location.builder(location)
			.setX(location.x + 50 * cos(heading))
			.setZ(location.z + 50 * sin(heading))
			.setHeading(heading)
		nextLocationBuilder.setY(ServerData.terrains.getHeight(nextLocationBuilder))
		return nextLocationBuilder.build()
	}
	
}