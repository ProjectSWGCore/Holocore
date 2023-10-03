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
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*

class JwksEndpoint(private val jwksUri: String) {
	private val factory = KeyFactory.getInstance("RSA")
	private val httpClient = HttpClient.newHttpClient()

	fun signatureCertificates(): Map<KID, RSAPublicKey> {
		val httpRequest = HttpRequest.newBuilder(URI(jwksUri)).GET().build()
		val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
		val statusCode = httpResponse.statusCode()
		if (statusCode != 200) {
			throw RuntimeException("Failed to retrieve signing certs, status code: $statusCode")
		}

		val jsonStr = httpResponse.body()
		val jsonObject = JSON.readObject(jsonStr)
		val signatureCertificates = mutableMapOf<KID, RSAPublicKey>()
		val keys = jsonObject.getArray("keys")
		for (key in keys) {
			val keyJsonObject = key as Map<String, String>
			val value = keyJsonObject["kid"] ?: continue
			val n = keyJsonObject["n"] ?: continue
			val e = keyJsonObject["e"] ?: continue

			if (keyJsonObject["use"] != "sig") {
				continue
			}

			signatureCertificates[KID(value)] = parseRSAPublicKey(n, e)
		}
		return signatureCertificates
	}

	private fun parseRSAPublicKey(n: String, e: String): RSAPublicKey {
		val urlDecoder = Base64.getUrlDecoder()
		val modulus = BigInteger(1, urlDecoder.decode(n))
		val exponent = BigInteger(1, urlDecoder.decode(e))
		val spec = RSAPublicKeySpec(modulus, exponent)

		return factory.generatePublic(spec) as RSAPublicKey
	}
}

data class KID(val value: String)