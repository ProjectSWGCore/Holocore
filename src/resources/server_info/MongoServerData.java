/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.server_info;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.projectswg.common.debug.Log;

import resources.server_info.SdbLoader.SdbResultSet;

public class MongoServerData implements Closeable {
	
	private final MongoClient client;
	private final MongoDatabase database;
	private final MongoCollection<Document> collection;
	
	public MongoServerData(String collection) {
		this.client = new MongoClient(new ServerAddress(), MongoClientOptions.builder().writeConcern(WriteConcern.UNACKNOWLEDGED).build());
		this.database = client.getDatabase("holocore");
		this.collection = database.getCollection(collection);
	}
	
	@Override
	public void close() {
		client.close();
	}
	
	public void importFromSdb(String sdb) {
		SdbLoader loader = new SdbLoader();
		try {
			SdbResultSet set = loader.load(new File("serverdata", sdb));
			List<String> columnList = set.getColumns();
			String [] columns = columnList.toArray(new String[columnList.size()]);
			Document doc;
			collection.drop();
			while (set.next()) {
				doc = new Document();
				for (int i = 0; i < columns.length; i++) {
					doc.put(columns[i], set.getObject(i));
				}
				collection.insertOne(doc);
			}
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
}
