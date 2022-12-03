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
import com.mongodb.client.model.*
import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.holocore.resources.support.data.persistable.SWGObjectFactory
import com.projectswg.holocore.resources.support.data.server_info.database.PswgObjectDatabase
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import org.bson.Document
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.stream.Collectors.toList

class PswgObjectDatabaseMongo(private val collection: MongoCollection<Document>) : PswgObjectDatabase {
	
	override val objects: List<MongoData>
		get() = collection.find(Filters.exists("deletedAt", false)).map { MongoData(it) }.into(ArrayList()) ?: ArrayList()
	
	init {
		collection.createIndex(Indexes.ascending("id"), IndexOptions().unique(true))
		collection.createIndex(Indexes.ascending("deletedAt"), IndexOptions().expireAfter(7, TimeUnit.DAYS))
	}
	
	override fun addObject(obj: SWGObject) {
		collection.replaceOne(Filters.eq("id", obj.objectId), SWGObjectFactory.save(obj, MongoData()).toDocument(), ReplaceOptions().upsert(true))
	}
	
	override fun addObjects(objects: Collection<SWGObject>) {
		if (objects.isEmpty())
			return
		collection.bulkWrite(objects.stream()
				.map { obj ->
					ReplaceOneModel(
							Filters.eq("id", obj.objectId),
							SWGObjectFactory.save(obj).toDocument(),
							ReplaceOptions().upsert(true)
					)
				}
				.collect(toList()),
				BulkWriteOptions().ordered(false))
	}
	
	override fun removeObject(id: Long): Boolean {
		return collection.updateOne(Filters.eq("id", id), Updates.set("deletedAt", Instant.now())).modifiedCount > 0
	}
	
	override fun getCharacterCount(account: String): Int {
		return collection.countDocuments(Filters.eq("account", account)).toInt()
	}
	
	override fun isCharacter(firstName: String): Boolean {
		return collection.countDocuments(Filters.and(
				Filters.regex("template", "object/creature/player/shared_.+\\.iff"),
				Filters.regex("base3.objectName", Pattern.compile(Pattern.quote(firstName) + "( .+|$)", Pattern.CASE_INSENSITIVE))
		)) > 0
	}
	
	override fun clearObjects(): Long {
		return collection.deleteMany(Filters.exists("_id")).deletedCount
	}
	
}
