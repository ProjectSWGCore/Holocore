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
package com.projectswg.holocore.resources.support.data.server_info.oidc

import com.projectswg.holocore.resources.support.data.server_info.database.UserMetadata
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import dasniko.testcontainers.keycloak.KeycloakContainer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.net.URI

class PswgUserDatabaseOIDCTest {

	companion object {
		// Could be any identity provider with support for OpenID Connect. Keycloak is easy to set up and run in a container.
		private val OIDCServer: KeycloakContainer = KeycloakContainer("quay.io/keycloak/keycloak:22.0.3")
			.withRealmImportFile("./swg-realm.json")
		private val pswgUserDatabaseOIDC: PswgUserDatabaseOIDC
			get() {
				// The realm name, client ID and client secret have been configured in the KeyCloak UI and saved to swg-realm.json
				val authorizationServerURI = "${OIDCServer.authServerUrl}/realms/swg"
				val wellKnownConfigurationURI = ".well-known/openid-configuration"
				return PswgUserDatabaseOIDC(authorizationServerURI, wellKnownConfigurationURI, "holocore-localhost", "nrrtW0NqbNWXn62ii6218QHWbEhVGXaf")
			}

		@JvmStatic
		@BeforeAll
		fun setUp() {
			OIDCServer.start()
		}

		@JvmStatic
		@AfterAll
		fun tearDown() {
			OIDCServer.stop()
		}
	}

	@Test
	fun auth_correct_credentials() {
		val authentication = pswgUserDatabaseOIDC.authenticate("user", "pass")
		val authenticated = authentication.success

		assertTrue(authenticated)
	}

	@Test
	fun userinfo() {
		val authentication = pswgUserDatabaseOIDC.authenticate("user", "pass")
		val actualUserMetadata = authentication.user ?: fail("User metadata is null, indicating a test setup error")
		val expectedUserMetadata = UserMetadata(
			accountId = "5a9e50a3-64b5-4650-a36d-c902fefa904a",
			username = "user",
			accessLevel = AccessLevel.DEV,
			isBanned = false
		)

		assertEquals(expectedUserMetadata, actualUserMetadata)
		assertEquals(expectedUserMetadata.accessLevel, actualUserMetadata.accessLevel)
	}

	@Test
	fun auth_incorrect_credentials() {
		val authentication = pswgUserDatabaseOIDC.authenticate("user", "wrong_pass")
		val authenticated = authentication.success

		assertFalse(authenticated)
	}

	@Test
	fun banned_user() {
		val authentication = pswgUserDatabaseOIDC.authenticate("banned-user", "pass")
		val userMetadata = authentication.user ?: fail("User metadata is null, indicating a test setup error")

		assertTrue(userMetadata.isBanned)
	}

}