/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.holocore.headless.*
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class XpLimitTest : AcceptanceTest() {

	@Test
	fun defaultLimitCapsXp() {
		val character = createCharacter()

		character.adminSetExperience("trapping", 5000)

		character.waitForExperiencePoints("trapping")
		assertEquals(2000, character.getXP("trapping"))
	}

	@Test
	fun skillBoxRaisesLimit() {
		val character = createCharacter()
		character.adminGrantSkill("outdoors_scout_tools_01")

		character.adminSetExperience("trapping", 5000)

		character.waitForExperiencePoints("trapping")
		assertEquals(5000, character.getXP("trapping"))
	}

	@Test
	fun noXpGainedWhenAtLimit() {
		val character = createCharacter()
		character.adminSetExperience("trapping", 2000)
		character.waitForExperiencePoints("trapping")

		character.adminSetExperience("trapping", 1000)

		assertEquals(2000, character.getXP("trapping"))
	}

	private fun createCharacter(): ZonedInCharacter {
		val user = generateUser(accessLevel = AccessLevel.DEV)
		return HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")
	}
}
