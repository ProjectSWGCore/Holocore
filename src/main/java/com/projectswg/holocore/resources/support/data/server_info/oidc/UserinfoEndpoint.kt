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

import me.joshlarson.json.JSON
import me.joshlarson.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class UserinfoEndpoint(private val userinfoEndpoint: String) {
	private val httpClient = HttpClient.newHttpClient()

	fun userinfo(accessToken: AccessToken): JSONObject {
		val type = accessToken.type
		val token = accessToken.token
		val userinfoHttpRequest = HttpRequest.newBuilder(URI(userinfoEndpoint))
			.header("Authorization", "$type $token")
			.GET()
			.build()
		val userinfoHttpResponse = httpClient.send(userinfoHttpRequest, HttpResponse.BodyHandlers.ofString())
		val statusCode = userinfoHttpResponse.statusCode()
		if (statusCode != 200) {
			throw RuntimeException("Failed to retrieve userinfo, status code: $statusCode")
		}
		val userinfoResponseBodyString = userinfoHttpResponse.body()
		return JSON.readObject(userinfoResponseBodyString)
	}
}