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
import com.mongodb.client.model.*
import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.holocore.resources.support.data.persistable.SWGObjectFactory
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import org.bson.Document
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors.toList

class PswgObjectDatabase(private val collection: MongoCollection<Document>?) {
	
	val objects: List<MongoData>
		get() = collection?.find()?.map { MongoData(it) }?.into(ArrayList()) ?: ArrayList()
	
	init {
		collection?.createIndex(Indexes.ascending("id"), IndexOptions().unique(true))
	}
	
	fun addObject(obj: SWGObject) {
		collection ?: return
		collection.replaceOne(Filters.eq("id", obj.objectId), SWGObjectFactory.save(obj, MongoData()).toDocument(), ReplaceOptions().upsert(true))
	}
	
	fun addObjects(objects: Collection<SWGObject>) {
		collection ?: return
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
	
	fun removeObject(id: Long): Boolean {
		collection ?: return true
		return collection.deleteOne(Filters.eq("id", id)).deletedCount > 0
	}
	
	fun getCharacterCount(account: String): Int {
		collection ?: return 0
		return collection.countDocuments(Filters.eq("account", account)).toInt()
	}
	
	fun isCharacter(firstName: String): Boolean {
		collection ?: return false
		return collection.countDocuments(Filters.and(
				Filters.regex("template", "object/creature/player/shared_.+\\.iff"),
				Filters.regex("base3.objectName", Pattern.compile(Pattern.quote(firstName) + "( .+|$)", Pattern.CASE_INSENSITIVE))
		)) > 0
	}
	
	fun clearObjects(): Long {
		collection ?: return 0
		return collection.deleteMany(Filters.exists("_id")).deletedCount
	}
	
}
