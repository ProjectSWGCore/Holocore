package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.projectswg.holocore.resources.support.data.server_info.database.*
import com.projectswg.holocore.resources.support.data.server_info.mariadb.PswgUserDatabaseMaria
import org.bson.Document

object PswgDatabase {
	
	private var configImpl = PswgConfigDatabase.createDefault()
	private var usersImpl = PswgUserDatabase.createDefault()
	private var objectsImpl = PswgObjectDatabase.createDefault()
	private var resourcesImpl = PswgResourceDatabase.createDefault()
	private var gcwRegionImpl = PswgGcwRegionDatabase.createDefault()
	
	val config: PswgConfigDatabase
		get() = configImpl
	val users: PswgUserDatabase
		get() = usersImpl
	val objects: PswgObjectDatabase
		get() = objectsImpl
	val resources: PswgResourceDatabase
		get() = resourcesImpl
	val gcwRegions: PswgGcwRegionDatabase
		get() = gcwRegionImpl
	
	fun initialize(connectionString: String, databaseName: String) {
		val client = MongoClients.create(connectionString)
		val database = client.getDatabase(databaseName)
		val databaseConfig = Database(database)
		
		val config = PswgConfigDatabaseMongo(databaseConfig.config.mongo)
		val users = initTable(databaseConfig.users, defaultCreator = {PswgUserDatabase.createDefault()}, mariaInitializer = ::PswgUserDatabaseMaria, mongoInitializer = ::PswgUserDatabaseMongo)
		val objects = initTable(databaseConfig.objects, defaultCreator = {PswgObjectDatabase.createDefault()}, mongoInitializer = ::PswgObjectDatabaseMongo)
		val resources = initTable(databaseConfig.resources, defaultCreator = {PswgResourceDatabase.createDefault()}, mongoInitializer = ::PswgResourceDatabaseMongo)
		val gcwRegions = initTable(databaseConfig.gcwRegions, defaultCreator = {PswgGcwRegionDatabase.createDefault()}, mongoInitializer = ::PswgGcwRegionDatabaseMongo)
		
		this.configImpl = config
		this.usersImpl = users
		this.objectsImpl = objects
		this.resourcesImpl = resources
		this.gcwRegionImpl = gcwRegions
	}
	
	private fun <T> initTable(table: DatabaseTable, defaultCreator: () -> T, mariaInitializer: (DatabaseTable) -> T = {defaultCreator()}, mongoInitializer: (MongoCollection<Document>) -> T = {defaultCreator()}): T {
		if (table.isMariaDefined())
			return mariaInitializer(table)
		return mongoInitializer(table.mongo)
	}
	
}
