/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
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
package com.projectswg.holocore.resources.support.data.location

import com.projectswg.common.data.location.Location
import java.util.function.BinaryOperator

/**
 * Reducer that determines the Location that is closest to a given base Location.
 */
class ClosestLocationReducer(private val baseLocation: Location) : BinaryOperator<Location?> {
	override fun apply(location1: Location?, location2: Location?): Location? {
		val terrainBase = baseLocation.terrain
		val terrain1 = location1?.terrain ?: return null
		val terrain2 = location2?.terrain ?: return null
		val terrain1Match = terrainBase == terrain1
		val terrain2Match = terrainBase == terrain2

		if (!terrain1Match && !terrain2Match) {
			// Given locations are both located on different planets
			return null
		}

		if (terrain1Match && !terrain2Match) {
			// Location 1 is the best fit since it has the same terrain as the base location while location 2 doesn't
			return location1
		} else if (!terrain1Match) {
			// Location 2 is the best fit since it has the same terrain as the base location while location 1 doesn't
			return location2
		}


		// location1 and location2 are on same terrain as baseLocation. Let's perform a distance check.
		val location1Distance = baseLocation.flatDistanceTo(location1)
		val location2Distance = baseLocation.flatDistanceTo(location2)

		return if (location1Distance > location2Distance) {
			location2 // Location 2 is closest to the player - return location2
		} else {
			location1 // Location 1 is closest to the player or both locations are equally close - return location1
		}
	}
}
