package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.mongodb.WriteConcern
import com.mongodb.client.MongoClients
import me.joshlarson.jlcommon.log.Log
import org.jetbrains.annotations.NotNull
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

object PswgDatabase {
	
	var config: PswgConfigDatabase? = null
		@NotNull get() = field ?: PswgConfigDatabase(null)
		private set
	var users: PswgUserDatabase? = null
		@NotNull get() = field!!
		private set
	var objects: PswgObjectDatabase? = null
		@NotNull get() = field!!
		private set
	var resources: PswgResourceDatabase? = null
		@NotNull get() = field!!
		private set
	
	fun initialize(connectionString: String, databaseName: String) {
		setupMongoLogging()
		val client = MongoClients.create(connectionString)
		val database = client.getDatabase(databaseName)
		
		config = PswgConfigDatabase(database.getCollection("config").withWriteConcern(WriteConcern.ACKNOWLEDGED))
		users = PswgUserDatabase(database.getCollection("users").withWriteConcern(WriteConcern.ACKNOWLEDGED))
		objects = PswgObjectDatabase(database.getCollection("objects").withWriteConcern(WriteConcern.JOURNALED))
		resources = PswgResourceDatabase(database.getCollection("resources").withWriteConcern(WriteConcern.ACKNOWLEDGED))
	}
	
	private fun setupMongoLogging() {
		var mongoLogger: Logger? = Logger.getLogger("com.mongodb")
		while (mongoLogger != null) {
			for (handler in mongoLogger.handlers) {
				mongoLogger.removeHandler(handler)
			}
			if (mongoLogger.parent != null)
				mongoLogger = mongoLogger.parent
			else
				break
		}
		mongoLogger?.addHandler(object : Handler() {
			override fun publish(record: LogRecord) {
				when (record.level) {
					Level.INFO -> Log.i("MongoDB: %s", record.message)
					Level.WARNING -> Log.w("MongoDB: %s", record.message)
					Level.SEVERE -> Log.e("MongoDB: %s", record.message)
					else -> Log.t("MongoDB: %s", record.message)
				}
			}
			
			override fun flush() {}
			override fun close() {}
		})
	}
	
}
