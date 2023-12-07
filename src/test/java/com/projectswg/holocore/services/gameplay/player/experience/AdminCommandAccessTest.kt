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

import com.projectswg.holocore.headless.CommandFailedException
import com.projectswg.holocore.headless.HeadlessSWGClient
import com.projectswg.holocore.headless.adminGrantSkill
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AdminCommandAccessTest : AcceptanceTest() {

	@Test
	fun adminsCanUseAdminCommands() {
		addUser("admin", "password", accessLevel = AccessLevel.DEV)
		val character = HeadlessSWGClient.createZonedInCharacter("admin", "password", "adminchar")

		assertDoesNotThrow { character.adminGrantSkill("outdoors_scout_master") }
	}

	@Test
	fun onlyAdminsCanUseAdminCommands() {
		addUser("player", "password", accessLevel = AccessLevel.PLAYER)
		val character = HeadlessSWGClient.createZonedInCharacter("player", "password", "playerchar")

		assertThrows<CommandFailedException> {
			character.adminGrantSkill("outdoors_scout_master")
		}
	}

}
