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

package com.projectswg.holocore.resources.support.data.server_info.mongodb.database;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.holocore.resources.support.data.persistable.SWGObjectFactory;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PswgObjectDatabase extends PswgDatabase {
	
	private MongoCollection<Document> collection;
	
	public PswgObjectDatabase() {
		this.collection = null;
	}
	
	@Override
	public void initialize() {
		super.initialize();
		this.collection = getCollectionByName("objects").withWriteConcern(WriteConcern.JOURNALED);
		
		this.collection.countDocuments();
		this.collection.createIndex(Indexes.ascending("id"), new IndexOptions().unique(true));
	}
	
	@Override
	public void terminate() {
		super.terminate();
		this.collection = null;
	}
	
	@NotNull
	public MongoCollection<Document> getCollection() {
		return collection;
	}
	
	public void addObject(@NotNull SWGObject obj) {
		collection.replaceOne(Filters.eq("id", obj.getObjectId()), SWGObjectFactory.save(obj, new MongoData()).toDocument(), new ReplaceOptions().upsert(true));
	}
	
	public void removeObject(long id) {
		collection.deleteOne(Filters.eq("id", id));
	}
	
	@NotNull
	public List<MongoData> getObjects() {
		return collection.find().map(MongoData::new).into(new ArrayList<>());
	}
	
}
