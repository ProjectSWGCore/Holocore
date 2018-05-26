package com.projectswg.holocore.resources.support.data.server_info.mongodb.users;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PswgUserDatabase extends PswgDatabase {
	
	private MongoCollection<Document> collection;
	
	public PswgUserDatabase() {
		this.collection = null;
	}
	
	@Override
	public void initialize() {
		super.initialize();
		this.collection = getCollectionByName("users");
		
		collection.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
		collection.createIndex(Indexes.ascending("characters.firstName"), new IndexOptions().unique(true));
		collection.count();
	}
	
	@Override
	public void terminate() {
		super.terminate();
		this.collection = null;
	}
	
	public MongoCollection<Document> getCollection() {
		return collection;
	}
	
/*
		getUser = database.prepareStatement("SELECT * FROM users WHERE LOWER(username) = LOWER(?)");
		getCharacter = database.prepareStatement("SELECT id FROM players WHERE LOWER(name) = ?");
		getCharacterFirstName = database.prepareStatement("SELECT id FROM players WHERE LOWER(name) = ? OR LOWER(name) LIKE ?");
		getCharacters = database.prepareStatement("SELECT * FROM players WHERE userid = ?");
		deleteCharacter = database.prepareStatement("DELETE FROM players WHERE id = ?");
 */
/*
						c.setId(set.getInt("id"));
						c.setName(set.getString("name"));
						c.setGalaxyId(ProjectSWG.getGalaxy().getId());
						c.setRaceCrc(Race.getRaceByFile(set.getString("race")).getCrc());
 */
	
	@Nullable
	public UserMetadata getUser(@NotNull String username) {
		return collection.find(Filters.eq("username", username)).map(UserMetadata::new).first();
	}
	
	@Nullable
	public CharacterMetadata getCharacter(long id) {
		return collection.find(Filters.eq("characters.id", id)).map(CharacterMetadata::new).first();
	}
	
	public boolean isCharacter(@NotNull String firstName) {
		return collection.count(Filters.eq("characters.firstName", firstName.toLowerCase(Locale.US)), new CountOptions().limit(1)) > 0;
	}
	
	@NotNull
	public List<CharacterMetadata> getCharacters(@NotNull String username) {
		return collection.aggregate(Arrays.asList(Aggregates.match(Filters.eq("username", username)), Aggregates.unwind("$characters"))).map(CharacterMetadata::new).into(new ArrayList<>());
	}
	
	public boolean deleteCharacter(long id) {
		return collection.updateOne(Filters.eq("characters.id", id), Updates.pull("characters", Filters.eq("id", id))).getModifiedCount() > 0;
	}
	
	public boolean insertCharacter(@NotNull String username, @NotNull CharacterMetadata character) {
		return collection.updateOne(Filters.eq("username", username), Updates.addToSet("characters", character.toDocument())).getModifiedCount() > 0;
	}
	
	public static class UserMetadata {
		
		private final String username;
		private final String password;
		private final String accessLevel;
		private final boolean banned;
		
		public UserMetadata(Document doc) {
			this.username = doc.getString("username");
			this.password = doc.getString("password");
			this.accessLevel = doc.getString("accessLevel");
			this.banned = doc.getBoolean("banned");
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
		
		public CharacterMetadata(Document doc) {
			doc = doc.get("characters", Document.class);
			this.id = doc.getLong("id");
			this.firstName = doc.getString("firstName");
			this.name = doc.getString("name");
			this.race = doc.getString("race");
		}
		
		public CharacterMetadata(long id, String firstName, String name, String race) {
			this.id = id;
			this.firstName = firstName;
			this.name = name;
			this.race = race;
		}
		
		public Document toDocument() {
			Document doc = new Document();
			doc.put("id", id);
			doc.put("firstName", firstName);
			doc.put("name", name);
			doc.put("race", race);
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
	}
	
}
