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
package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.holocore.headless.*
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HealthWoundTest : AcceptanceTest() {

	@BeforeEach
	fun setUpUser() {
		addUser("username", "password")
	}

	@Test
	fun `weapons with Wound Chance apply health wounds`() {
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")
		character.player.creatureObject.equippedWeapon.woundChance = 100F
		val npc = spawnNPC("creature_bantha", character.player.creatureObject.location, combatLevelRange = 80..80)

		character.attack(npc)

		assertTrue(npc.healthWounds > 0)
	}

	@Test
	fun `only weapons with Wound Chance apply health wounds`() {
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")
		character.player.creatureObject.equippedWeapon.woundChance = 0F
		val npc = spawnNPC("creature_bantha", character.player.creatureObject.location, combatLevelRange = 80..80)

		character.attack(npc)

		assertTrue(npc.healthWounds == 0)
	}

	@Test
	fun `health wounds are subtracted from current health`() {
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")
		val npc = spawnNPC("creature_bantha", character.player.creatureObject.location, combatLevelRange = 80..80)
		npc.healthWounds = npc.health - 1    // The NPC should effectively have 1 health left this way

		val targetState = character.attack(npc)

		assertEquals(TargetState.DEAD, targetState)
	}

}
