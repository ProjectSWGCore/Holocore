/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.services.support.npc.ai

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.resources.support.npc.ai.NavigationOffset
import com.projectswg.holocore.test.runners.TestRunnerNoIntents
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class TestAIMovementService: TestRunnerNoIntents() {
	
	@Test
	fun testOffsetLocation() {
		assertEquals(0.0,   headingTo(0.0, 1.0), 1E-7, "NORTH")
		assertEquals(270.0, headingTo(1.0, 0.0), 1E-7, "EAST")
		assertEquals(180.0, headingTo(0.0, -1.0), 1E-7, "SOUTH")
		assertEquals(90.0,  headingTo(-1.0, 0.0), 1E-7, "WEST")
		
		testOffset(headingTo(0.0, 1.0),   1.0, 1.0)
		testOffset(headingTo(-1.0, 0.0),  -1.0, 1.0)
		testOffset(headingTo(0.0, -1.0), -1.0, -1.0)
		testOffset(headingTo(1.0, 0.0), 1.0, -1.0)
		
		testOffset(headingTo(1.0, 1.0),   sqrt(2.0), 0.0)
	}
	
	private fun headingTo(eX: Double, eZ: Double): Double {
		return Location.builder().setPosition(0.0, 0.0, 0.0).build().getHeadingTo(Location.builder().setPosition(eX, 0.0, eZ).build())
	}
	
	private fun testOffset(heading: Double, tx: Double, tz: Double) {
		val startLocation = Location.builder().setPosition(0.0, 0.0, 0.0).build()
		val endLocation = AIMovementService.offsetLocation(startLocation, heading, NavigationOffset(1.0, 1.0))
		assertEquals(tx, endLocation.x, 1E-7, "X")
		assertEquals(tz, endLocation.z, 1E-7, "Z")
	}
	
}
