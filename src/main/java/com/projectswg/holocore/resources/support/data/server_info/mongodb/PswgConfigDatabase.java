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

package com.projectswg.holocore.resources.support.data.server_info.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import me.joshlarson.jlcommon.log.Log;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PswgConfigDatabase implements PswgDatabase {
	
	private MongoCollection<Document> collection;
	
	PswgConfigDatabase() {
		this.collection = null;
	}
	
	@Override
	public void open(MongoCollection<Document> collection) {
		this.collection = collection;
		collection.createIndex(Indexes.ascending("package"), new IndexOptions().unique(true));
	}
	
	@NotNull
	public MongoCollection<Document> getCollection() {
		return collection;
	}
	
	public String getString(Object o, String key, String def) {
		for (Document config : getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getString(key);
		}
		return def;
	}
	
	public boolean getBoolean(Object o, String key, boolean def) {
		for (Document config : getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getBoolean(key);
		}
		return def;
	}
	
	public int getInt(Object o, String key, int def) {
		for (Document config : getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getInteger(key);
		}
		return def;
	}
	
	public double getDouble(Object o, String key, double def) {
		for (Document config : getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getDouble(key);
		}
		return def;
	}
	
	public double getLong(Object o, String key, long def) {
		for (Document config : getConfigurations(o)) {
			if (config.containsKey(key))
				return config.getLong(key);
		}
		return def;
	}
	
	private List<Document> getConfigurations(Object o) {
		String packageKey = o instanceof Class ? ((Class<?>) o).getPackageName() : Objects.requireNonNull(o).getClass().getPackageName();
		if (packageKey.startsWith("com.projectswg.holocore."))
			packageKey = packageKey.substring(24);
		else if (packageKey.startsWith("com.projectswg.holocore"))
			packageKey = packageKey.substring(23);
		else
			throw new IllegalArgumentException("package lookup object does not belong to holocore");
		
		if (packageKey.startsWith("intents."))
			throw new IllegalArgumentException("intents should not be querying configs");
		
		if (packageKey.startsWith("resources.") || packageKey.startsWith("services."))
			packageKey = packageKey.substring(packageKey.indexOf('.')+1);
		
		List<Document> configs = new ArrayList<>();
		if (collection == null)
			return configs;
		while (!packageKey.isEmpty()) {
			Document doc = collection.find(Filters.eq("package", packageKey)).first();
			if (doc != null)
				configs.add(doc);
			
			int lastDot = packageKey.lastIndexOf('.');
			packageKey = lastDot == -1 ? "" : packageKey.substring(0, lastDot);
		}
		return configs;
	}
	
}
