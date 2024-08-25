/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.combat.buffs

import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import org.junit.jupiter.api.Test

class Publish24MovementSystemTest {
	@Test
	fun rootBeforeSnare() {
		val testRoot1 = ServerData.movements.getMovement("testRoot1")!!
		val testWebSnare1 = ServerData.movements.getMovement("testWebSnare1")!!
		val movementModifiers = listOf(testRoot1, testWebSnare1)
		
		val selectedModifier = Publish24MovementSystem.selectMovementModifier(movementModifiers)
		
		assert(selectedModifier == testRoot1)
	}
	
	@Test
	fun rootBeforeBoost() {
		val testRoot1 = ServerData.movements.getMovement("testRoot1")!!
		val testMedBoost1 = ServerData.movements.getMovement("testMedBoost1")!!
		val movementModifiers = listOf(testRoot1, testMedBoost1)
		
		val selectedModifier = Publish24MovementSystem.selectMovementModifier(movementModifiers)
		
		assert(selectedModifier == testRoot1)
	}
	
	@Test
	fun rootBeforePermaboost() {
		val testRoot1 = ServerData.movements.getMovement("testRoot1")!!
		val testItemBoost1 = ServerData.movements.getMovement("testItemBoost1")!!
		val movementModifiers = listOf(testRoot1, testItemBoost1)
		
		val selectedModifier = Publish24MovementSystem.selectMovementModifier(movementModifiers)
		
		assert(selectedModifier == testRoot1)
	}
	
	@Test
	fun snareBeforeBoost() {
		val testWebSnare1 = ServerData.movements.getMovement("testWebSnare1")!!
		val testItemBoost1 = ServerData.movements.getMovement("testItemBoost1")!!
		val movementModifiers = listOf(testWebSnare1, testItemBoost1)
		
		val selectedModifier = Publish24MovementSystem.selectMovementModifier(movementModifiers)
		
		assert(selectedModifier == testWebSnare1)
	}
	
	@Test
	fun strongestSnare() {
		val testItemSnare1 = ServerData.movements.getMovement("testItemSnare1")!!
		val testWebSnare1 = ServerData.movements.getMovement("testWebSnare1")!!
		val testWebSnare2 = ServerData.movements.getMovement("testWebSnare2")!!
		val movementModifiers = listOf(testItemSnare1, testWebSnare1, testWebSnare2)
		
		val selectedModifier = Publish24MovementSystem.selectMovementModifier(movementModifiers)
		
		assert(selectedModifier == testWebSnare2)
	}
	
	@Test
	fun strongestPermasnare() {
		val testItemSnare1 = ServerData.movements.getMovement("testItemSnare1")!!
		val testItemSnare2 = ServerData.movements.getMovement("testItemSnare2")!!
		val movementModifiers = listOf(testItemSnare1, testItemSnare2)
		
		val selectedModifier = Publish24MovementSystem.selectMovementModifier(movementModifiers)
		
		assert(selectedModifier == testItemSnare2)
	}
	
	@Test
	fun strongestBoost() {
		val testMedBoost1 = ServerData.movements.getMovement("testMedBoost1")!!
		val testMedBoost2 = ServerData.movements.getMovement("testMedBoost2")!!
		val movementModifiers = listOf(testMedBoost1, testMedBoost2)
		
		val selectedModifier = Publish24MovementSystem.selectMovementModifier(movementModifiers)
		
		assert(selectedModifier == testMedBoost2)
	}
	
	@Test
	fun strongestPermaboost() {
		val testItemBoost1 = ServerData.movements.getMovement("testItemBoost1")!!
		val testItemBoost2 = ServerData.movements.getMovement("testItemBoost2")!!
		val movementModifiers = listOf(testItemBoost1, testItemBoost2)
		
		val selectedModifier = Publish24MovementSystem.selectMovementModifier(movementModifiers)
		
		assert(selectedModifier == testItemBoost2)
	}
	
}