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
package com.projectswg.holocore.services.support.global.chat

import com.projectswg.common.data.encodables.chat.ChatResult
import com.projectswg.holocore.headless.HeadlessSWGClient
import com.projectswg.holocore.headless.addIgnore
import com.projectswg.holocore.headless.sendTell
import com.projectswg.holocore.headless.waitForTell
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TellTest : AcceptanceTest() {

	@Test
	fun `tell is received by player`() {
		addUser("username", "password", AccessLevel.DEV)
		val character1 = HeadlessSWGClient.createZonedInCharacter("username", "password", "Charone")
		val character2 = HeadlessSWGClient.createZonedInCharacter("username", "password", "Chartwo")

		val chatResult = character1.sendTell("Chartwo", "Hello")
		assertEquals(ChatResult.SUCCESS, chatResult)

		val tell = character2.waitForTell()
		assertAll(
			{ assertEquals("Charone", tell.sender) },
			{ assertEquals("Hello", tell.message) },
		)
	}

	@Test
	fun `receiving character name is case insensitive`() {
		addUser("username", "password", AccessLevel.DEV)
		val character1 = HeadlessSWGClient.createZonedInCharacter("username", "password", "Charone")
		HeadlessSWGClient.createZonedInCharacter("username", "password", "Chartwo")

		val chatResult = character1.sendTell("CHARTWO", "Hello")

		assertEquals(ChatResult.SUCCESS, chatResult)
	}

	@Test
	fun `tells can only be sent to online players`() {
		addUser("username", "password", AccessLevel.DEV)
		val character1 = HeadlessSWGClient.createZonedInCharacter("username", "password", "Charone")
		val swgClient = HeadlessSWGClient("username")
		val characterSelectionScreen = swgClient.login("password")
		characterSelectionScreen.createCharacter("Chartwo")    // Create character but don't zone in

		val chatResult = character1.sendTell("Chartwo", "Hello")

		assertEquals(ChatResult.TARGET_AVATAR_DOESNT_EXIST, chatResult)
	}

	@Test
	fun `drop tells to non-existent characters`() {
		addUser("username", "password", AccessLevel.DEV)
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "charone")

		val chatResult = character.sendTell("chartwo", "Hello")

		assertEquals(ChatResult.TARGET_AVATAR_DOESNT_EXIST, chatResult)
	}

	@Test
	fun `drop tells from ignored characters`() {
		addUser("username", "password", AccessLevel.DEV)
		val character1 = HeadlessSWGClient.createZonedInCharacter("username", "password", "charone")
		val character2 = HeadlessSWGClient.createZonedInCharacter("username", "password", "chartwo")
		character2.addIgnore("charone")

		val chatResult = character1.sendTell("chartwo", "Hello")

		assertEquals(ChatResult.IGNORED, chatResult)
	}
}