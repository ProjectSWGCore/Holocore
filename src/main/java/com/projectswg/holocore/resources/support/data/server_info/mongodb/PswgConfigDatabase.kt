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
import java.util.*

/**
 * Empty config database that only returns the defaults
 */
class PswgConfigDatabase(private val collection: MongoCollection<Document>?) {
	
	init {
		collection?.createIndex(Indexes.ascending("package"), IndexOptions().unique(true))
	}
	
	fun getString(o: Any, key: String, def: String): String {
		collection ?: return def
		for (config in getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getString(key)
		}
		return def
	}
	
	fun getBoolean(o: Any, key: String, def: Boolean): Boolean {
		collection ?: return def
		for (config in getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getBoolean(key)!!
		}
		return def
	}
	
	fun getInt(o: Any, key: String, def: Int): Int {
		collection ?: return def
		for (config in getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getInteger(key)!!
		}
		return def
	}
	
	fun getDouble(o: Any, key: String, def: Double): Double {
		collection ?: return def
		for (config in getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getDouble(key)!!
		}
		return def
	}
	
	fun getLong(o: Any, key: String, def: Long): Long {
		collection ?: return def
		for (config in getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getLong(key)!!
		}
		return def
	}
	
	private fun getConfigurations(o: Any): List<Document> {
		collection!! // Should be verified in calling functions
		var packageKey = if (o is Class<*>) o.packageName else o.javaClass.packageName
		require(packageKey.startsWith("com.projectswg.holocore")) { "packageKey must be a part of holocore, was: $packageKey" }
		
		packageKey = packageKey.removePrefix("com.projectswg.holocore")
		packageKey = packageKey.removePrefix(".")
		
		if (packageKey.startsWith("intents."))
			throw IllegalArgumentException("intents should not be querying configs")
		
		if (packageKey.startsWith("resources.") || packageKey.startsWith("services."))
			packageKey = packageKey.substringAfter('.')
		
		val configs = ArrayList<Document>()
		
		while (packageKey.isNotEmpty()) {
			val doc = collection.find(Filters.eq("package", packageKey)).first()
			if (doc != null)
				configs.add(doc)
			
			if (!packageKey.contains('.'))
				break
			packageKey = packageKey.substringBeforeLast('.')
		}
		return configs
	}
	
}
