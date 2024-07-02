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
package com.projectswg.holocore.services.support

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.headless.HeadlessSWGClient
import com.projectswg.holocore.headless.adminKill
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KillAdminCommandTest : AcceptanceTest() {

	@Test
	fun killNpc() {
		val user = generateUser(AccessLevel.DEV)
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")
		val npc = spawnNPC("creature_bantha", character.player.creatureObject.location, NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE)

		character.adminKill(npc)

		assertEquals(Posture.DEAD, npc.posture)
	}

	@Test
	fun killInvulnerableNpc() {
		val user = generateUser(AccessLevel.DEV)
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")
		val npc = spawnNPC("creature_bantha", character.player.creatureObject.location, NpcStaticSpawnLoader.SpawnerFlag.INVULNERABLE)

		character.adminKill(npc)

		assertAll(
			{ assertEquals(Posture.DEAD, npc.posture, "admins should be able to kill invulnerable NPCs with /kill") },
			{ assertTrue(character.player.playerObject.experience.isEmpty(), "admins should not gain experience from killing invulnerable NPCs") }
		)
	}

	@Test
	fun killSelf() {
		val user = generateUser(AccessLevel.DEV)
		val character = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "adminchar")

		character.adminKill(character.player.creatureObject)

		assertNotEquals(Posture.DEAD, character.player.creatureObject)
	}

	@Test
	fun killPlayer() {
		addUser("username", "password", AccessLevel.DEV)
		val character1 = HeadlessSWGClient.createZonedInCharacter("username", "password", "charone")
		val character2 = HeadlessSWGClient.createZonedInCharacter("username", "password", "chartwo")

		character1.adminKill(character2.player.creatureObject)

		assertEquals(Posture.DEAD, character2.player.creatureObject.posture)
	}

}
