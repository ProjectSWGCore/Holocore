/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects.swg.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerObjectFactionPointTest {
	private lateinit var playerObject: PlayerObject

	@BeforeEach
	fun setUp() {
		playerObject = PlayerObject(0)
	}

	@Test
	fun adjustment() {
		val adjustment = playerObject.adjustFactionPoints("rebel", 100)
		assertEquals(100, adjustment)
	}

	@Test
	fun `getFactionPoints reflects changes made with adjustFactionPoints`() {
		playerObject.adjustFactionPoints("rebel", 100)
		playerObject.adjustFactionPoints("rebel", 50)

		val factionPoints = playerObject.factionPoints

		assertEquals(150, factionPoints["rebel"])
	}

	@Test
	fun `going above upper bound is not possible`() {
		val adjustment = playerObject.adjustFactionPoints("rebel", 5015)
		assertEquals(5000, adjustment)
	}

	@Test
	fun `going below lower bound is not possible`() {
		val adjustment = playerObject.adjustFactionPoints("rebel", -5015)
		assertEquals(-5000, adjustment)
	}
}