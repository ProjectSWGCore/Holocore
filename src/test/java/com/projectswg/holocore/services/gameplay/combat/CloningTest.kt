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
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.services.gameplay.combat.cloning.CloningService
import com.projectswg.holocore.services.gameplay.combat.duel.DuelService
import com.projectswg.holocore.services.support.objects.ObjectStorageService
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class CloningTest : AcceptanceTest() {

	@BeforeEach
	fun setupExtraServices() {
		registerService(CloningService())
		registerService(DuelService())
		registerService(ObjectStorageService())
		waitForIntents() // ObjectStorageService floods the server with intents during initialization, causing LoginService to not respond to login requests before these intents are processed
	}

	@Test
	fun `possible to clone after being deathblown`() {
		addUser("username", "password", AccessLevel.DEV)
		val character1 = HeadlessSWGClient.createZonedInCharacter("username", "password", "charone")
		val character2 = HeadlessSWGClient.createZonedInCharacter("username", "password", "chartwo")
		character1.duel(character2.player.creatureObject)
		character2.duel(character1.player.creatureObject)
		character1.adminKill(character2.player.creatureObject)
		character1.deathblow(character2.player.creatureObject)

		val suiWindow = character2.waitForCloneActivation()
		suiWindow.select(0)    // The facility we clone at doesn't matter. We just need to make sure that cloning is possible.
		suiWindow.clickOk()

		assertDoesNotThrow { character2.waitForObjectMove() }
	}
}