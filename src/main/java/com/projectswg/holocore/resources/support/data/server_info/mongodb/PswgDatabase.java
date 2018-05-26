package com.projectswg.holocore.resources.support.data.server_info.mongodb;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class PswgDatabase {
	
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	
	public void initialize() {
		if (initialized.getAndSet(true))
			throw new IllegalStateException("Already initialized");
		PswgDatabaseConnectionPool.INSTANCE.initialize();
	}
	
	public void terminate() {
		if (!initialized.getAndSet(false))
			throw new IllegalStateException("Already terminated");
		PswgDatabaseConnectionPool.INSTANCE.terminate();
	}
	
	protected MongoCollection<Document> getCollectionByName(String name) {
		return PswgDatabaseConnectionPool.INSTANCE.getCollectionByName(name);
	}
	
}
