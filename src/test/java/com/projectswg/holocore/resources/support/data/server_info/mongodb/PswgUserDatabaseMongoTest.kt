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
package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.projectswg.holocore.resources.support.data.server_info.database.PswgUserDatabase
import org.bson.Document
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PswgUserDatabaseMongoTest : PswgDatabaseTestHarness<PswgUserDatabase>("users", ::PswgUserDatabaseMongo) {

	@Test
	fun `unknown user is null`() {
		val username = "deathbringer7"

		val userMetadata = items.getUser(username)

		assertNull(userMetadata)
	}

	@Test
	fun `known user is found`() {
		val username = "deathbringer7"
		val hashedPassword = "\$2a\$10\$DpHgnWS6iBL3hAZIo/Cbmev8pkB3sERtl8MTAZniYG3lG9mZoSlQS"
		insertUser(username, hashedPassword)

		val userMetadata = items.getUser(username)

		assertNotNull(userMetadata)
	}
	
	@Test
	fun `user with plaintext password can be authenticated`() {
		val username = "laxguy6"
		val password = "plaintext_password"
		insertUser(username, password)
		val userMetadata = items.getUser(username) ?: fail("Unable to retrieve test user")

		val authenticated = items.authenticate(userMetadata, password)

		assertTrue(authenticated)
	}
	
	@Test
	fun `user with hashed password can be authenticated`() {
		val username = "deathbringer7"
		val password = "thebestpassword"
		val hashedPassword = "\$2a\$10\$DpHgnWS6iBL3hAZIo/Cbmev8pkB3sERtl8MTAZniYG3lG9mZoSlQS"
		insertUser(username, hashedPassword)
		val userMetadata = items.getUser(username) ?: fail("Unable to retrieve test user")

		val authenticated = items.authenticate(userMetadata, password)

		assertTrue(authenticated)
	}
	
	@Test
	fun `wrong password is rejected`() {
		val username = "deathbringer7"
		val hashedPassword = "\$2a\$10\$DpHgnWS6iBL3hAZIo/Cbmev8pkB3sERtl8MTAZniYG3lG9mZoSlQS"
		insertUser(username, hashedPassword)
		val userMetadata = items.getUser(username) ?: fail("Unable to retrieve test user")

		val authenticated = items.authenticate(userMetadata, "wrong_password")

		assertFalse(authenticated)
	}

	private fun insertUser(username: String, password: String) {
		val document = Document()
		document["username"] = username
		document["accessLevel"] = "player"
		document["banned"] = false
		document["password"] = password
		collection.insertOne(document)
	}
}
