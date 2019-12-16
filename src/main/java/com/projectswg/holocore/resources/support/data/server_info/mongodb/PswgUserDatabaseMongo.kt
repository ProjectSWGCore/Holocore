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

package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.projectswg.holocore.resources.support.data.server_info.database.PswgUserDatabase
import com.projectswg.holocore.resources.support.data.server_info.database.UserMetadata
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import org.bson.Document

class PswgUserDatabaseMongo(private val mongoCollection: MongoCollection<Document>) : PswgUserDatabase {
	
	init {
		mongoCollection.createIndex(Indexes.ascending("username"), IndexOptions().unique(true))
	}
	
	override fun getUser(username: String): UserMetadata? {
		return mongoCollection
				.find(Filters.eq("username", username))
				.map { createUserMetadata(it) }
				.first()
	}
	
	private fun createUserMetadata(doc: Document): UserMetadata {
		val accessLevel = when (doc.getString("accessLevel")) {
			"warden" -> AccessLevel.WARDEN
			"csr" -> AccessLevel.CSR
			"qa" -> AccessLevel.QA
			"dev" -> AccessLevel.DEV
			else -> AccessLevel.PLAYER
		}
		return UserMetadata(accountId = doc.getObjectId("_id").toHexString(),
							username = doc.getString("username"),
							password = doc.getString("password"),
							accessLevel = accessLevel,
							isBanned = doc.getBoolean("banned"))
	}
	
}
