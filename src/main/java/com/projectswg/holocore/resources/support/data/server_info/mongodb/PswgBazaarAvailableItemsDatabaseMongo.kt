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
package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.holocore.resources.support.data.server_info.database.PswgBazaarAvailableItemsDatabase
import org.bson.Document
import java.time.LocalDateTime
import java.time.ZoneOffset

class PswgBazaarAvailableItemsDatabaseMongo(private val collection: MongoCollection<Document>) : PswgBazaarAvailableItemsDatabase {

	private val zoneOffset = ZoneOffset.UTC
	
	init {
		collection.createIndex(Indexes.ascending("itemObjectId"), IndexOptions().unique(true))
		collection.createIndex(Indexes.ascending("ownerId"), IndexOptions().unique(false))
	}

	override fun getAvailableItem(itemObjectId: Long): PswgBazaarAvailableItemsDatabase.AvailableItemMetadata? {
		return collection.find(Filters.eq("itemObjectId", itemObjectId)).map { documentToAvailableItemMetadata(it) }.first()
	}

	override fun addAvailableItem(availableItemMetadata: PswgBazaarAvailableItemsDatabase.AvailableItemMetadata) {
		val itemObjectId = availableItemMetadata.itemObjectId
		val document = toDocument(availableItemMetadata)

		collection.replaceOne(Filters.eq("itemObjectId", itemObjectId), document, ReplaceOptions().upsert(true))
	}

	override fun getMyAvailableItems(ownerId: Long): Collection<PswgBazaarAvailableItemsDatabase.AvailableItemMetadata> {
		return collection.find(Filters.eq("ownerId", ownerId)).map { documentToAvailableItemMetadata(it) }.toList()
	}

	override fun removeAvailableItem(availableItemMetadata: PswgBazaarAvailableItemsDatabase.AvailableItemMetadata) {
		collection.deleteOne(Filters.eq("itemObjectId", availableItemMetadata.itemObjectId))
	}

	private fun toDocument(availableItemMetadata: PswgBazaarAvailableItemsDatabase.AvailableItemMetadata): Document {
		val mongoData = MongoData()
		mongoData.putLong("itemObjectId", availableItemMetadata.itemObjectId)
		mongoData.putInteger("price", availableItemMetadata.price)
		mongoData.putDate("expiresAt", availableItemMetadata.expiresAt.toInstant(zoneOffset))
		mongoData.putString("description", availableItemMetadata.description)
		mongoData.putLong("ownerId", availableItemMetadata.ownerId)
		mongoData.putLong("bazaarObjectId", availableItemMetadata.bazaarObjectId)
		mongoData.putString("saleType", availableItemMetadata.saleType.name)
		return mongoData.toDocument()
	}

	private fun documentToAvailableItemMetadata(document: Document): PswgBazaarAvailableItemsDatabase.AvailableItemMetadata {
		return PswgBazaarAvailableItemsDatabase.AvailableItemMetadata(
			itemObjectId = document.getLong("itemObjectId"),
			price = document.getInteger("price"),
			expiresAt = LocalDateTime.ofInstant(document.getDate("expiresAt").toInstant(), zoneOffset),
			description = document.getString("description"),
			ownerId = document.getLong("ownerId"),
			bazaarObjectId = document.getLong("bazaarObjectId"),
			saleType = PswgBazaarAvailableItemsDatabase.AvailableItemSaleType.valueOf(document.getString("saleType"))
		)
	}
}