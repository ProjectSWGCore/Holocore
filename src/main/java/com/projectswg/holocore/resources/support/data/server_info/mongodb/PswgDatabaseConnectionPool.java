package com.projectswg.holocore.resources.support.data.server_info.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.projectswg.common.data.info.Config;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import me.joshlarson.jlcommon.log.Log;
import org.bson.Document;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

class PswgDatabaseConnectionPool {
	
	static final PswgDatabaseConnectionPool INSTANCE = new PswgDatabaseConnectionPool();
	
	private final AtomicInteger initializeVotes;
	private MongoClient client;
	private MongoDatabase database;
	
	public PswgDatabaseConnectionPool() {
		this.initializeVotes = new AtomicInteger(0);
		this.client = null;
		this.database = null;
	}
	
	public synchronized void initialize() {
		if (initializeVotes.getAndIncrement() > 0)
			return;
		Config primary = DataManager.getConfig(ConfigFile.PRIMARY);
		String database = primary.getString("MONGODB-DB", "nge");
		
		setupMongoLogging();
		
		this.client = MongoClients.create(primary.getString("MONGODB", "mongodb://localhost"));
		this.database = client.getDatabase(database);
		
		// Blocking operation to force setup the connection
		this.database.listCollectionNames();
	}
	
	public synchronized void terminate() {
		if (initializeVotes.decrementAndGet() > 0)
			return;
		this.client.close();
		
		this.client = null;
		this.database = null;
	}
	
	public MongoCollection<Document> getCollectionByName(String name) {
		return database.getCollection(name);
	}
	
	private static void setupMongoLogging() {
		Logger mongoLogger = Logger.getLogger("com.mongodb");
		while (mongoLogger != null) {
			for (Handler handler : mongoLogger.getHandlers()) {
				mongoLogger.removeHandler(handler);
			}
			if (mongoLogger.getParent() != null)
				mongoLogger = mongoLogger.getParent();
			else
				break;
		}
		if (mongoLogger != null) {
			mongoLogger.addHandler(new Handler() {
				public void publish(LogRecord record) {
					Level level = record.getLevel();
					if (level.equals(Level.INFO))
						Log.i("MongoDB: %s", record.getMessage());
					else if (level.equals(Level.WARNING))
						Log.w("MongoDB: %s", record.getMessage());
					else if (level.equals(Level.SEVERE))
						Log.e("MongoDB: %s", record.getMessage());
					else
						Log.t("MongoDB: %s", record.getMessage());
				}
				public void flush() { }
				public void close() throws SecurityException { }
			});
		}
	}
	
}
