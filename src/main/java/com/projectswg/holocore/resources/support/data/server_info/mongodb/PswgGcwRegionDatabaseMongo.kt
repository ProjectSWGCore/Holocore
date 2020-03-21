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
import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.holocore.resources.support.data.server_info.database.PswgGcwRegionDatabase
import com.projectswg.holocore.resources.support.data.server_info.database.ZoneMetadata
import org.bson.Document

class PswgGcwRegionDatabaseMongo(private val collection: MongoCollection<Document>) : PswgGcwRegionDatabase {
	
	init {
		collection.createIndex(Indexes.ascending("zone"), IndexOptions().unique(true))
	}

	override fun createZone(zoneName: String, basePoints: Long) {
		val mongoData = MongoData()

		// Initiualize a zone with some points. We do this so gaining 100% control can't be done by being the first to receive any amount of GCW points.
		mongoData.putString("zone", zoneName)
		mongoData.putLong("imperialPoints", basePoints)
		mongoData.putLong("rebelPoints", basePoints)

		collection.insertOne(mongoData.toDocument())
	}

	override fun setImperialPoints(zoneName: String, points: Long) {
		val mongoData = MongoData()

		mongoData.putLong("imperialPoints", points)

		collection.updateOne(Filters.eq("zone", zoneName), Document("\$set", mongoData.toDocument()))
	}

	override fun setRebelPoints(zoneName: String, points: Long) {
		val mongoData = MongoData()

		mongoData.putLong("rebelPoints", points)

		collection.updateOne(Filters.eq("zone", zoneName), Document("\$set", mongoData.toDocument()))
	}

	override fun getZone(zoneName: String): ZoneMetadata? {
		return collection
				.find(Filters.eq("zone", zoneName))
				.map { ZoneMetadata(
						zone=it.getString("zone"),
						imperialPoints=it.getLong("imperialPoints"),
						rebelPoints=it.getLong("rebelPoints")
				)}
				.first()
	}
	
}
