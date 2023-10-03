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

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.projectswg.holocore.resources.support.data.server_info.database.Authentication
import com.projectswg.holocore.resources.support.data.server_info.database.PswgUserDatabase
import com.projectswg.holocore.resources.support.data.server_info.database.UserMetadata
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import me.joshlarson.jlcommon.log.Log
import me.joshlarson.json.JSONObject


/**
 * Implements a user database with the OAuth2 Password Grant flow to retrieve user information from a remote authorization server.
 *
 * Based on the OpenID Connect specification: https://openid.net/specs/openid-connect-core-1_0.html
 *
 * @param authorizationServerBaseURI The base URI of the authorization server, e.g. "http://localhost:8080/realms/swg"
 * @param wellKnownConfigurationURI relative URI to the well-known configuration, e.g. ".well-known/openid-configuration"
 * @param clientId The client ID of the client registered with the authorization server
 * @param clientSecret The client secret of the client registered with the authorization server
 */
class PswgUserDatabaseOIDC(private val authorizationServerBaseURI: String, wellKnownConfigurationURI: String, clientId: String, clientSecret: String) : PswgUserDatabase {

	private val openIDConnect = OpenIDConnect(authorizationServerBaseURI, wellKnownConfigurationURI, clientId, clientSecret)

	override fun authenticate(username: String, password: String): Authentication {
		return try {
			attemptCredentials(username, password)
		} catch (exception: Exception) {
			Log.w("User authentication failed: ${exception.message}")
			Authentication(false, null)
		}
	}

	private fun attemptCredentials(username: String, password: String): Authentication {
		val passwordGrant = openIDConnect.tokenEndpoint.passwordGrant(username, password)
		val idToken = passwordGrant.idToken

		verifyAccessToken(idToken)

		val accessToken = passwordGrant.accessToken
		val userinfo = openIDConnect.userinfoEndpoint.userinfo(accessToken)
		val userMetadata = mapJsonObjectToUserMetadata(userinfo, username)
		return Authentication(true, userMetadata)
	}

	private fun verifyAccessToken(idToken: IdToken) {
		val signatureCertificates = openIDConnect.jwksEndpoint.signatureCertificates()
		val decodedJWT = idToken.token
		val kid = KID(decodedJWT.keyId)
		val rsaPublicKey = signatureCertificates[kid]
		val algorithm = Algorithm.RSA256(rsaPublicKey, null)
		val verifier = JWT.require(algorithm).withIssuer(authorizationServerBaseURI).build()

		verifier.verify(decodedJWT)	// Exception is thrown if verification fails
	}

	private fun mapJsonObjectToUserMetadata(userinfo: JSONObject, username: String): UserMetadata {
		val accessLevel = accessLevel(userinfo, username)
		val banned = isBanned(userinfo)
		val accountId = userinfo.getString("sub")
		return UserMetadata(accountId, username, accessLevel, banned)
	}

	private fun isBanned(userinfo: JSONObject): Boolean {
		val claimName = "banned"
		if (userinfo.containsKey(claimName)) {
			return userinfo.getBoolean(claimName)
		}

		return false
	}

	private fun accessLevel(userinfo: JSONObject, username: String): AccessLevel {
		if (userinfo.containsKey("roles")) {
			val roles = userinfo.getArray("roles")
			if (roles.isNotEmpty()) {
				return largestAccessLevel(roles)
			}
		}

		return fallbackAccessLevel(username)
	}

	private fun largestAccessLevel(roles: MutableList<Any>): AccessLevel {
		return roles.map { it as String }.map { AccessLevel.valueOf(it) }.maxBy { it.value }
	}

	private fun fallbackAccessLevel(username: String): AccessLevel {
		val fallbackAccessLevel = AccessLevel.PLAYER
		Log.w("User $username has no roles assigned, defaulting to $fallbackAccessLevel")
		return fallbackAccessLevel
	}

}
