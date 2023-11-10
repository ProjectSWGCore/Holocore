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

import com.projectswg.holocore.headless.*
import com.projectswg.holocore.services.support.global.zone.LoginService
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoginTest : TestRunnerSimulatedWorld() {

	private val memoryUserDatabase = MemoryUserDatabase()

	@BeforeEach
	fun setUp() {
		registerService(LoginService(memoryUserDatabase))
	}

	@AfterEach
	fun tearDown() {
		memoryUserDatabase.clear()
	}

	@Test
	fun validCredentials() {
		memoryUserDatabase.addUser("username", "password")
		val headlessSWGClient = HeadlessSWGClient("username")

		val characterSelectionScreen = headlessSWGClient.login("password")

		assertNotNull(characterSelectionScreen)
	}

	@Test
	fun validCredentialsButBanned() {
		memoryUserDatabase.addUser("username", "password", banned = true)
		val headlessSWGClient = HeadlessSWGClient("username")

		assertThrows<AccountBannedException> { headlessSWGClient.login("password") }
	}

	@Test
	fun wrongUsername() {
		memoryUserDatabase.addUser("username", "password")
		val headlessSWGClient = HeadlessSWGClient("wrongusername")

		assertThrows<WrongCredentialsException> { headlessSWGClient.login("password") }
	}

	@Test
	fun wrongPassword() {
		memoryUserDatabase.addUser("username", "password")
		val headlessSWGClient = HeadlessSWGClient("username")

		assertThrows<WrongCredentialsException> { headlessSWGClient.login("wrongpassword") }
	}

	@Test
	fun wrongVersion() {
		memoryUserDatabase.addUser("username", "password")
		val headlessSWGClient = HeadlessSWGClient("username", "20030404-14:00")

		assertThrows<WrongClientVersionException> { headlessSWGClient.login("password") }
	}


}