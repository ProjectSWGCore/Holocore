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
package com.projectswg.holocore.services.support

import com.projectswg.holocore.headless.HeadlessSWGClient
import com.projectswg.holocore.test.runners.IntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CharacterManagementTest : IntegrationTest() {

	@Test
	fun deleteCharacter() {
		addUser("username", "password")
		val headlessSWGClient = HeadlessSWGClient("username")
		val characterSelectionScreen = headlessSWGClient.login("password")
		val characterId = characterSelectionScreen.createCharacter("firstcharacter")

		characterSelectionScreen.deleteCharacter(characterId)

		assertFalse(characterSelectionScreen.characters.contains(characterId))
	}

	@Test
	fun characterCreationRateLimit() {
		addUser("username", "password")
		val headlessSWGClient = HeadlessSWGClient("username")
		val characterSelectionScreen = headlessSWGClient.login("password")
		characterSelectionScreen.createCharacter("firstcharacter")
		characterSelectionScreen.createCharacter("secondcharacter")

		val exception = assertThrows<Exception> { characterSelectionScreen.createCharacter("thirdcharacter") }
		assertTrue(exception.message!!.lowercase().contains("too_fast"))
	}

}