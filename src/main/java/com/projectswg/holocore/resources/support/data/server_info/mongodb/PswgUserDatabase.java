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
import com.mongodb.client.model.*;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PswgUserDatabase implements PswgDatabase {
	
	private MongoCollection<Document> collection;
	
	PswgUserDatabase() {
		this.collection = null;
	}
	
	@Override
	public void open(MongoCollection<Document> collection) {
		this.collection = collection;
		collection.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
		collection.createIndex(Indexes.ascending("characters.firstName"), new IndexOptions().unique(true).partialFilterExpression(Filters.type("characters.firstName", "string")));
	}
	
	@Nullable
	public UserMetadata getUser(@NotNull String username) {
		return collection.find(Filters.eq("username", username)).map(UserMetadata::new).first();
	}
	
	@Nullable
	public CharacterMetadata getCharacter(long id) {
		return collection.find(Filters.eq("characters.id", id)).map(CharacterMetadata::new).first();
	}
	
	public boolean isCharacter(@NotNull String firstName) {
		return collection.countDocuments(Filters.eq("characters.firstName", firstName.toLowerCase(Locale.US)), new CountOptions().limit(1)) > 0;
	}
	
	@NotNull
	public List<CharacterMetadata> getCharacters(@NotNull String username) {
		return collection.aggregate(Arrays.asList(Aggregates.match(Filters.eq("username", username)), Aggregates.unwind("$characters"))).map(CharacterMetadata::new).into(new ArrayList<>());
	}
	
	public boolean deleteCharacters() {
		return collection.updateMany(Filters.exists("characters"), Updates.unset("characters")).getModifiedCount() > 0;
	}
	
	public boolean deleteCharacter(String username, long id) {
		return collection.updateOne(Filters.and(Filters.eq("username", username), Filters.eq("characters.id", id)), Updates.pull("characters", Filters.eq("id", id))).getModifiedCount() > 0;
	}
	
	public boolean deleteCharacter(long id) {
		return collection.updateOne(Filters.eq("characters.id", id), Updates.pull("characters", Filters.eq("id", id))).getModifiedCount() > 0;
	}
	
	public boolean insertCharacter(@NotNull String username, @NotNull CharacterMetadata character) {
		return collection.updateOne(Filters.eq("username", username), Updates.addToSet("characters", character.toDocument())).getModifiedCount() > 0;
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
	
	public static class CharacterMetadata {
		
		private final long id;
		private final String firstName;
		private final String name;
		private final String race;
		private final Document detailedData;
		
		public CharacterMetadata(Document doc) {
			doc = doc.get("characters", Document.class);
			this.id = doc.getLong("id");
			this.firstName = doc.getString("firstName");
			this.name = doc.getString("name");
			this.race = doc.getString("race");
			this.detailedData = doc.get("detail", Document.class);
		}
		
		public CharacterMetadata(long id, String firstName, String name, String race, Document detailedData) {
			this.id = id;
			this.firstName = firstName;
			this.name = name;
			this.race = race;
			this.detailedData = detailedData;
		}
		
		public Document toDocument() {
			Document doc = new Document();
			doc.put("id", id);
			doc.put("firstName", firstName);
			doc.put("name", name);
			doc.put("race", race);
			doc.put("detailedData", detailedData);
			return doc;
		}
		
		public long getId() {
			return id;
		}
		
		public String getFirstName() {
			return firstName;
		}
		
		public String getName() {
			return name;
		}
		
		public String getRace() {
			return race;
		}
		
		public Map<String, Object> getDetailedData() {
			return Collections.unmodifiableMap(detailedData);
		}
		
	}
	
}
