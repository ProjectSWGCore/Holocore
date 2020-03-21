/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.data.server_info.mariadb

import com.projectswg.holocore.resources.support.data.server_info.database.DatabaseTable
import com.projectswg.holocore.resources.support.data.server_info.database.PswgUserDatabase
import com.projectswg.holocore.resources.support.data.server_info.database.UserMetadata
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import me.joshlarson.jlcommon.log.Log
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class PswgUserDatabaseMaria(private val database: DatabaseTable): PswgUserDatabase {
	
	private val getUserStatement = ThreadLocal.withInitial { database.mariaConnection!!.prepareStatement("SELECT userID, password, banned FROM ${database.mariaTable!!} WHERE username = $1") }
	
	override fun getUser(username: String): UserMetadata? {
		try {
			val accessLevel = when (database.configuration?.accessLevels?.get(username)) {
				"warden" -> AccessLevel.WARDEN
				"csr" -> AccessLevel.CSR
				"qa" -> AccessLevel.QA
				"dev" -> AccessLevel.DEV
				else -> AccessLevel.PLAYER
			}
			val statement = getUserStatement.get()
			statement.setString(1, username)
			statement.executeQuery().use { set ->
				return UserMetadata(accountId = set.getInt("userId").toString(16).toUpperCase(Locale.US),
									username = username,
									password = set.getString("password") ?: return null,
									accessLevel = accessLevel,
									isBanned = set.getInt("banned") != 0)
			}
		} catch (e: SQLException) {
			Log.w("SQLException when looking up user: $username")
			Log.w(e)
		}
		return null
	}
	
	private inline fun (ResultSet).use(op: (ResultSet) -> Unit) {
		@Suppress("ConvertTryFinallyToUseCall")
		try {
			op(this)
		} finally {
			close()
		}
	}
	
}
