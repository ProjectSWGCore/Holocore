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
package com.projectswg.holocore.services.gameplay.player.experience

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HealthAddedCalculatorTest {
	private val combatLevel1 = CombatLevel(1, 500, 0)
	private val combatLevel2 = CombatLevel(2, 1000, 50)
	private val combatLevel3 = CombatLevel(2, 6000, 75)
	private val healthAddedCalculator = HealthAddedCalculator()

	@Test
	fun healthIsDecreased_whenLevelIsDecreased() {
		val healthChange = healthAddedCalculator.calculate(combatLevel3, combatLevel1)
		Assertions.assertEquals(-75, healthChange)
	}

	@Test
	fun healthIsIncreased_whenLevelIsIncreased() {
		val healthChange = healthAddedCalculator.calculate(combatLevel2, combatLevel3)
		Assertions.assertEquals(25, healthChange)
	}

	@Test
	fun healthIsUnchanged_whenLevelIsUnchanged() {
		val healthChange = healthAddedCalculator.calculate(combatLevel2, combatLevel2)
		Assertions.assertEquals(0, healthChange)
	}
}