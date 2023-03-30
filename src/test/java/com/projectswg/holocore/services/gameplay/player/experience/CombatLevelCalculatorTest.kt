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

class CombatLevelCalculatorTest {
	private val combatLevel1 = CombatLevel(1, 500, 0)
	private val combatLevel2 = CombatLevel(2, 1000, 50)
	private val combatLevel3 = CombatLevel(3, 6000, 75)
	private val repository = FakeCombatLevelRepository()
	private val calculator = CombatLevelCalculator(repository)

	@Test
	fun combatLevelIsSelected_whenRequiredXpIsMatchedExactly() {
		repository.addCombatLevel(combatLevel1)
		repository.addCombatLevel(combatLevel2)
		repository.addCombatLevel(combatLevel3)
		val exactXpForCombatLevel2 = combatLevel2.requiredCombatXp
		val combatLevel = calculator.calculate(exactXpForCombatLevel2)
		Assertions.assertEquals(combatLevel2, combatLevel)
	}

	@Test
	fun smallestCombatLevelIsSelected_whenPlayerHasTooLittleXp() {
		repository.addCombatLevel(combatLevel1)
		repository.addCombatLevel(combatLevel2)
		repository.addCombatLevel(combatLevel3)
		val notEnoughXpForCombatLevel1 = 10
		val combatLevel = calculator.calculate(notEnoughXpForCombatLevel1)
		Assertions.assertEquals(combatLevel1, combatLevel)
	}

	@Test
	fun biggestCombatLevelIsSelected_whenPlayerHasMuchXp() {
		repository.addCombatLevel(combatLevel1)
		repository.addCombatLevel(combatLevel2)
		repository.addCombatLevel(combatLevel3)
		val muchXp = 100000
		val combatLevel = calculator.calculate(muchXp)
		Assertions.assertEquals(combatLevel3, combatLevel)
	}
}