package com.projectswg.utility;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConvertLogin {
	
	public static void main(String [] args) throws ClassNotFoundException, SQLException {
		// super("org.sqlite.JDBC", "jdbc:sqlite:" + file);
		Class.forName("org.sqlite.JDBC");
		System.out.println("Connecting to sqlite...");
		try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:serverdata/login/login.db")) {
			System.out.println("Connected");
			MongoClient client = new MongoClient();
			MongoDatabase database = client.getDatabase("nge");
			MongoCollection<Document> loginCollection = database.getCollection("users");
			
			System.out.println("Mongo initialized.");
			
			Map<Integer, String> users = new HashMap<>();
			try (ResultSet set = sqlite.createStatement().executeQuery("SELECT * FROM users")) {
				while (set.next()) {
					String username = set.getString("username");
					String password = set.getString("password");
					String accessLevel = set.getString("access_level");
					boolean banned = set.getBoolean("banned");
					users.put(set.getInt("id"), username);
					if (loginCollection.count(Filters.eq("username", username)) > 0)
						continue;
					System.out.println("Transferring user " + username);
					Document doc = new Document();
					doc.put("username", username);
					doc.put("password", password);
					doc.put("accessLevel", accessLevel);
					doc.put("banned", banned);
					doc.put("characters", new ArrayList<>());
					loginCollection.insertOne(doc);
				}
			}
			try (ResultSet set = sqlite.createStatement().executeQuery("SELECT * FROM players")) {
				while (set.next()) {
					int userId = set.getInt("userid");
					long id = set.getLong("id");
					String name = set.getString("name");
					String race = set.getString("race");
					String firstName = name.toLowerCase(Locale.US);
					if (firstName.indexOf(' ') != -1)
						firstName = firstName.substring(0, firstName.indexOf(' '));
					if (loginCollection.count(Filters.eq("characters.firstName", firstName)) > 0)
						continue;
					
					System.out.println("Transferring character " + name);
					Document doc = new Document();
					doc.put("id", id);
					doc.put("firstName", firstName);
					doc.put("name", name);
					doc.put("race", race);
					loginCollection.updateOne(Filters.eq("username", users.get(userId)), Updates.addToSet("characters", doc));
				}
			}
			
			client.close();
		}
	}
	
}
