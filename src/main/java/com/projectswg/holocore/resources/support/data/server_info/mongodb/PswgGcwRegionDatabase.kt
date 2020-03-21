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
import org.bson.Document

class PswgGcwRegionDatabase(private val collection: MongoCollection<Document>?) {
	
	init {
		collection?.createIndex(Indexes.ascending("zone"), IndexOptions().unique(true))
	}

	fun createZone(zoneName: String) {
		collection ?: return

		val mongoData = MongoData()

		// Initiualize a zone with some points. We do this so gaining 100% control can't be done by being the first to receive any amount of GCW points.
		mongoData.putString("zone", zoneName)
		mongoData.putLong("imperialPoints", 50_000)
		mongoData.putLong("rebelPoints", 50_000)

		collection.insertOne(mongoData.toDocument())
	}

	fun addImperialPoints(zoneName: String, points: Long) {
		collection ?: return
		val mongoData = MongoData()

		mongoData.putLong("imperialPoints", points)

		collection.updateOne(Filters.eq("zone", zoneName), Document("\$set", mongoData.toDocument()))
	}

	fun addRebelPoints(zoneName: String, points: Long) {
		collection ?: return
		val mongoData = MongoData()

		mongoData.putLong("rebelPoints", points)

		collection.updateOne(Filters.eq("zone", zoneName), Document("\$set", mongoData.toDocument()))
	}

	fun getZone(zoneName: String): ZoneMetadata? {
		return collection
				?.find(Filters.eq("zone", zoneName))
				?.map { ZoneMetadata(it) }
				?.first()
	}
	
}

class ZoneMetadata(doc: Document) {
	
	val _id: String = doc.getObjectId("_id").toHexString()
	val zone: String = doc.getString("zone")
	val imperialPoints: Long = doc.getLong("imperialPoints")
	val rebelPoints: Long = doc.getLong("rebelPoints")

	override fun toString(): String = "ZoneMetadata[zone=$zone imperialPoints=$imperialPoints rebelPoints=$rebelPoints]"
	override fun equals(other: Any?): Boolean = if (other is ZoneMetadata) zone == other.zone else false
	override fun hashCode(): Int = zone.hashCode()
	
}
