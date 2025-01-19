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
package com.projectswg.holocore.services.gameplay.player.experience

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.headless.*
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrappingXPTest : AcceptanceTest() {

	@Test
	fun scoutsGetTrappingXPFromKillingCreatures() {
		val user = generateUser()
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")
		val npc = spawnNPC("creature_bantha", character.player.creatureObject.location)

		killTarget(character, npc)

		// There are three types of XP we receive from this kill: combat_meleespecialize_unarmed, trapping, and combat_general
		character.waitForExperiencePoints("trapping")
		assertTrue(character.getXP("trapping") > 0)
	}

	@Test
	fun onlyScoutsGetTrappingXPFromKillingCreatures() {
		val user = generateUser()
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")
		character.surrenderSkill("outdoors_scout_novice")
		val npc = spawnNPC("creature_bantha", character.player.creatureObject.location)

		killTarget(character, npc)

		assertEquals(0, character.getXP("trapping"))
	}

	@Test
	fun scoutsGetNoTrappingXPFromKillingNonCreatures() {
		val user = generateUser()
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")
		val npc = spawnNPC("humanoid_tusken_commoner", character.player.creatureObject.location)

		killTarget(character, npc)

		assertEquals(0, character.getXP("trapping"))
	}

	private fun killTarget(character: ZonedInCharacter, target: CreatureObject) {
		target.health = 1
		character.attack(target)
		assertEquals(Posture.DEAD, character.waitUntilPostureUpdate(target))
	}

}
