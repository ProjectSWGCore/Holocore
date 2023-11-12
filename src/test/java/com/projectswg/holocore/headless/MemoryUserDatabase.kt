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
package com.projectswg.holocore.headless

import com.projectswg.holocore.resources.support.data.server_info.database.PswgUserDatabase
import com.projectswg.holocore.resources.support.data.server_info.database.UserMetadata
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import java.util.*

class MemoryUserDatabase : PswgUserDatabase {

	private val usernameToUserMetadata = mutableMapOf<String, UserMetadata>()
	private val userMetadataToPassword = mutableMapOf<UserMetadata, String>()

	override fun getUser(username: String): UserMetadata? {
		return usernameToUserMetadata[username]
	}

	override fun authenticate(userMetadata: UserMetadata, password: String): Boolean {
		return userMetadataToPassword[userMetadata] == password
	}

	fun addUser(username: String, password: String, accessLevel: AccessLevel = AccessLevel.PLAYER, banned: Boolean = false) {
		if (usernameToUserMetadata.containsKey(username))
			throw IllegalArgumentException("User already exists")

		val userMetadata = UserMetadata(UUID.randomUUID().toString(), username, accessLevel, banned)
		usernameToUserMetadata[userMetadata.username] = userMetadata
		userMetadataToPassword[userMetadata] = password
	}

	fun clear() {
		usernameToUserMetadata.clear()
		userMetadataToPassword.clear()
	}
}
