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

import com.mongodb.assertions.Assertions
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.test.runners.TestRunnerNoIntents
import org.junit.jupiter.api.Test

class TestDynamicMovementProcessor : TestRunnerNoIntents() {

	@Test
	fun testNoBuildZoneCheck() {
		// No intersections
		Assertions.assertFalse(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(5391, -3185), tatooine(-559, -1197)))
		Assertions.assertFalse(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(5391, -3185), tatooine(5710, -5478)))
		Assertions.assertFalse(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(5391, -3185), tatooine(4433, -4013)))
		Assertions.assertFalse(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(3577, -1067), tatooine(-2141, 4485)))
		Assertions.assertFalse(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(3577, -1067), tatooine(5812, 5580)))
		
		// Intersections
		Assertions.assertTrue(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(5391, -3185), tatooine(5812, 5580)))
		Assertions.assertTrue(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(5391, -3185), tatooine(3505, -4811)))
		Assertions.assertTrue(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(5391, -3185), tatooine(2503, -5798)))
		Assertions.assertTrue(DynamicMovementProcessor.isIntersectingProtectedZone(tatooine(5391, -3185), tatooine(-5246, -3984)))
	}
	
	private fun tatooine(x: Int, z: Int): Location {
		return Location.builder().setTerrain(Terrain.TATOOINE).setX(x.toDouble()).setZ(z.toDouble()).build()
	}

}
