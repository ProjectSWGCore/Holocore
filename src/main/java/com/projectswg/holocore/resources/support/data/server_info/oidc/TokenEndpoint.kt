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
import com.auth0.jwt.interfaces.DecodedJWT
import me.joshlarson.json.JSON
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TokenEndpoint(private val grantTypesSupported: Collection<String>, private val tokenEndpoint: String, private val authorizationHeaderValue: String) {
	private val httpClient = HttpClient.newHttpClient()

	fun passwordGrant(username: String, password: String): TokenResponse {
		if (!grantTypesSupported.contains("password")) {
			throw RuntimeException("Password grant type is not supported by the identity provider")
		}

		val tokenRequest = HttpRequest.newBuilder(URI(tokenEndpoint))
			.header("Authorization", authorizationHeaderValue)
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString("grant_type=password&username=$username&password=$password&scope=openid"))
			.build()
		val tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString())
		val statusCode = tokenResponse.statusCode()
		if (statusCode != 200 && statusCode != 401) {
			throw RuntimeException("Failed to invoke token endpoint, unexpected status code: $statusCode")
		}
		val tokenResponseBodyString = tokenResponse.body()
		val tokenJsonObject = JSON.readObject(tokenResponseBodyString)
		val accessToken = tokenJsonObject.getString("access_token")
		val idTokenStr = tokenJsonObject.getString("id_token")
		val tokenType = tokenJsonObject.getString("token_type")
		val idToken = JWT.decode(idTokenStr)

		return TokenResponse(AccessToken(accessToken, tokenType), IdToken(idToken, tokenType))
	}
}

// This object could be extended to include the other fields in the token response, but these are all we need right now
data class TokenResponse(val accessToken: AccessToken, val idToken: IdToken)
data class AccessToken(val token: String, val type: String)
data class IdToken(val token: DecodedJWT, val type: String)
