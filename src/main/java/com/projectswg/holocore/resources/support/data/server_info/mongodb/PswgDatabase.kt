package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.mongodb.WriteConcern
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import me.joshlarson.jlcommon.log.Log
import org.bson.Document
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.reflect.KProperty

object PswgDatabase {
	
	private val database = ObjectRef<MongoDatabase?>(null)
	val config by DatabaseDelegate(database, "config") { PswgConfigDatabase(it?.withWriteConcern(WriteConcern.ACKNOWLEDGED)) }
	val users by DatabaseDelegate(database, "users") { PswgUserDatabase(it?.withWriteConcern(WriteConcern.ACKNOWLEDGED)) }
	val objects by DatabaseDelegate(database, "objects") { PswgObjectDatabase(it?.withWriteConcern(WriteConcern.JOURNALED)) }
	val resources by DatabaseDelegate(database, "resources") { PswgResourceDatabase(it?.withWriteConcern(WriteConcern.ACKNOWLEDGED)) }
	
	fun initialize(connectionString: String, databaseName: String) {
		setupMongoLogging()
		val client = MongoClients.create(connectionString)
		val database = client.getDatabase(databaseName)
		
		this.database.element = database
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
	
	private class DatabaseDelegate<T>(private val database: ObjectRef<MongoDatabase?>, private val collectionName: String, private var databaseSupplier: (MongoCollection<Document>?) -> T) {
		
		private var prevDatabase: MongoDatabase? = null
		private var prevReturn: T = databaseSupplier(null)
		
		operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
			val database = this.database.element
			if (prevDatabase != database) {
				this.prevDatabase = database
				this.prevReturn = databaseSupplier(database?.getCollection(collectionName))
			}
			return prevReturn
		}
		
	}
	
	private class ObjectRef<T: Any?>(var element: T)
	
}
