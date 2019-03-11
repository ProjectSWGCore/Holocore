package com.projectswg.holocore.resources.support.data.server_info.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.joshlarson.jlcommon.log.Log;
import org.bson.Document;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public interface PswgDatabase {
	
	default void open(MongoCollection<Document> collection) {
		
	}
	
	static void initialize(String connectionString, String databaseName) {
		setupMongoLogging(); 
		MongoClient client = MongoClients.create(connectionString);
		MongoDatabase database = client.getDatabase(databaseName);
		for (PswgDatabaseImpl db : PswgDatabaseImpl.values()) {
			db.open(database);
		}
	}
	
	static PswgConfigDatabase config() {
		return (PswgConfigDatabase) PswgDatabaseImpl.CONFIG.getImplementation();
	}
	
	static PswgUserDatabase users() {
		return (PswgUserDatabase) PswgDatabaseImpl.USERS.getImplementation();
	}
	
	static PswgObjectDatabase objects() {
		return (PswgObjectDatabase) PswgDatabaseImpl.OBJECTS.getImplementation();
	}
	
	static PswgResourceDatabase resources() {
		return (PswgResourceDatabase) PswgDatabaseImpl.RESOURCES.getImplementation();
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
