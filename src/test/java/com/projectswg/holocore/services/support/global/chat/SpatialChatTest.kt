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
package com.projectswg.holocore.services.support.global.chat

import com.projectswg.holocore.headless.*
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpatialChatTest : AcceptanceTest() {

	@Test
	fun `spatial chat is received by nearby player`() {
		val user = generateUser()
		val character1 = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "Charone")
		val character2 = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "Chartwo")
		val message = "Hello there!"

		character1.sendSpatialChat(message)

		val spatialChat = character2.waitForSpatialChat()
		assertAll(
			{ assertEquals(character1.player.creatureObject.objectId, spatialChat.sourceId) },
			{ assertEquals(message, spatialChat.message) },
		)
	}

	@Test
	fun `no spatial chat from ignored players`() {
		val user = generateUser()
		val character1 = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "Charone")
		val character2 = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "Chartwo")
		val message = "Hello there!"

		character2.addIgnore(character1.player.characterFirstName)
		character1.sendSpatialChat(message)

		assertThrows<NoSpatialChatReceivedException> { character2.waitForSpatialChat() }
	}
}
