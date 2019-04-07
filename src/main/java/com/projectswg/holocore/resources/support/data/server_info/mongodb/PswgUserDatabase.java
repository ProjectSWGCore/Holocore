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
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PswgUserDatabase implements PswgDatabase {
	
	private MongoCollection<Document> collection;
	
	PswgUserDatabase() {
		this.collection = null;
	}
	
	@Override
	public void open(MongoCollection<Document> collection) {
		this.collection = collection;
		collection.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
	}
	
	@Nullable
	public UserMetadata getUser(@NotNull String username) {
		return collection.find(Filters.eq("username", username)).map(UserMetadata::new).first();
	}
	
	public static class UserMetadata {
		
		private final String accountId;
		private final String username;
		private final String password;
		private final String accessLevel;
		private final boolean banned;
		
		public UserMetadata(Document doc) {
			this.accountId = doc.getObjectId("_id").toHexString();
			this.username = doc.getString("username");
			this.password = doc.getString("password");
			this.accessLevel = doc.getString("accessLevel");
			this.banned = doc.getBoolean("banned");
		}
		
		public String getAccountId() {
			return accountId;
		}
		
		public String getUsername() {
			return username;
		}
		
		public String getPassword() {
			return password;
		}
		
		public String getAccessLevel() {
			return accessLevel;
		}
		
		public boolean isBanned() {
			return banned;
		}
		
	}
	
}
