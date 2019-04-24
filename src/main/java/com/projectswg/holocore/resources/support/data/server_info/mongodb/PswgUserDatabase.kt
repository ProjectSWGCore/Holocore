/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */

package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.bson.Document

class PswgUserDatabase(private val collection: MongoCollection<Document>) {
	
	init {
		collection.createIndex(Indexes.ascending("username"), IndexOptions().unique(true))
	}
	
	fun getUser(username: String): UserMetadata? {
		return collection
				.find(Filters.eq("username", username))
				.map { UserMetadata(it) }
				.first()
	}
	
}

class UserMetadata(doc: Document) {
	
	val accountId: String = doc.getObjectId("_id").toHexString()
	val username: String = doc.getString("username")
	val password: String = doc.getString("password")
	val accessLevel: String = doc.getString("accessLevel")
	val isBanned: Boolean = doc.getBoolean("banned")
	
	override fun toString(): String = "UserMetadata[username=$username accessLevel=$accessLevel banned=$isBanned]"
	override fun equals(other: Any?): Boolean = if (other is UserMetadata) accountId == other.accountId else false
	override fun hashCode(): Int = accountId.hashCode()
	
}
