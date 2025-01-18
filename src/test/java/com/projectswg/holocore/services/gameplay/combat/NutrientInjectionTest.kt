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
package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.holocore.headless.*
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NutrientInjectionTest : AcceptanceTest() {

	@Test
	fun buffSelf() {
		val zonedInCharacter1 = createMasterMedic()
		zonedInCharacter1.waitForHealthChange() // Wait for health update for becoming a master medic
		val char1OriginalMaxHealth = zonedInCharacter1.player.creatureObject.maxHealth

		zonedInCharacter1.sendSelfBuffCommand("nutrientInjection")

		val char1NewMaxHealth = zonedInCharacter1.player.creatureObject.maxHealth
		assertTrue(char1NewMaxHealth > char1OriginalMaxHealth)
	}

	@Test
	fun buffFriendlyTarget() {
		val zonedInCharacter1 = createMasterMedic()
		val zonedInCharacter2 = createZonedInCharacter("Chartwo")
		val char2OriginalMaxHealth = zonedInCharacter2.player.creatureObject.maxHealth
		
		zonedInCharacter1.waitUntilAwareOf(zonedInCharacter2.player.creatureObject)
		zonedInCharacter1.sendTargetBuffCommand("nutrientInjection", zonedInCharacter2.player.creatureObject)

		val char2NewMaxHealth = zonedInCharacter2.player.creatureObject.maxHealth
		assertTrue(char2NewMaxHealth > char2OriginalMaxHealth)
	}

	private fun createMasterMedic(): ZonedInCharacter {
		val zonedInCharacter1 = createZonedInCharacter("Charone")
		zonedInCharacter1.adminGrantSkill("science_medic_master")
		return zonedInCharacter1
	}

	private fun createZonedInCharacter(characterName: String): ZonedInCharacter {
		val user = generateUser(accessLevel = AccessLevel.DEV)
		return HeadlessSWGClient.createZonedInCharacter(user.username, user.password, characterName)
	}

}

