/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.support.data.server_info.database.PswgResourceDatabase
import org.bson.Document
import java.util.stream.Collectors.toList

class PswgResourceDatabaseMongo(private val collection: MongoCollection<Document>) : PswgResourceDatabase {
	
	override var resources: List<GalacticResource>
		get() = collection.find().map { MongoData.create(it) { GalacticResource() } }.into(ArrayList()) ?: ArrayList()
		set(value) {
			if (value.isEmpty())
				return

			collection.bulkWrite(value.stream()
					.map { resource ->
						ReplaceOneModel(
								Filters.eq("id", resource.id), // match the resource id
								MongoData.store(resource).toDocument(), // store the resource into a mongodb document
								ReplaceOptions().upsert(true) // replace any matches with the new resource
						)
					}
					.collect(toList()),
					BulkWriteOptions().ordered(false))
		}
	
	init {
		collection.createIndex(Indexes.ascending("id"), IndexOptions().unique(true))
	}
	
}
