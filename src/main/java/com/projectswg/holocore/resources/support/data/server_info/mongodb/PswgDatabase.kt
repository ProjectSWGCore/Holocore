/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.mongodb

import com.mongodb.client.MongoClients
import com.projectswg.holocore.resources.support.data.server_info.database.*
import me.joshlarson.jlcommon.log.Log
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

object PswgDatabase {
	
	private var configImpl = PswgConfigDatabase.createDefault()
	private var usersImpl = PswgUserDatabase.createDefault()
	private var objectsImpl = PswgObjectDatabase.createDefault()
	private var resourcesImpl = PswgResourceDatabase.createDefault()
	private var bazaarInstantSalesImpl = PswgBazaarInstantSalesDatabase.createDefault()
	private var bazaarAvailableItemsImpl = PswgBazaarAvailableItemsDatabase.createDefault()
	private var chatRoomsImpl = PswgChatRoomDatabase.createDefault()
	
	val config: PswgConfigDatabase
		get() = configImpl
	val users: PswgUserDatabase
		get() = usersImpl
	val objects: PswgObjectDatabase
		get() = objectsImpl
	val resources: PswgResourceDatabase
		get() = resourcesImpl
	val bazaarInstantSales: PswgBazaarInstantSalesDatabase
		get() = bazaarInstantSalesImpl
	val bazaarAvailableItems: PswgBazaarAvailableItemsDatabase
		get() = bazaarAvailableItemsImpl
	val chatRooms: PswgChatRoomDatabase
		get() = chatRoomsImpl
	
	fun initialize(connectionString: String, databaseName: String) {
		setupMongoLogging()
		val client = MongoClients.create(connectionString)
		val mongo = client.getDatabase(databaseName)

		val config = PswgConfigDatabaseMongo(mongo.getCollection("config"))
		val users = PswgUserDatabaseMongo(mongo.getCollection("users"))
		val objects = PswgObjectDatabaseMongo(mongo.getCollection("objects"))
		val resources = PswgResourceDatabaseMongo(mongo.getCollection("resources"))
		val bazaarInstantSales = PswgBazaarInstantSalesDatabaseMongo(mongo.getCollection("bazaarInstantSales"))
		val bazaarAvailableItems = PswgBazaarAvailableItemsDatabaseMongo(mongo.getCollection("bazaarAvailableItems"))
		val chatRooms = PswgChatRoomDatabaseMongo(mongo.getCollection("chatRooms"))
		
		this.configImpl = config
		this.usersImpl = users
		this.objectsImpl = objects
		this.resourcesImpl = resources
		this.bazaarInstantSalesImpl = bazaarInstantSales
		this.bazaarAvailableItemsImpl = bazaarAvailableItems
		this.chatRoomsImpl = chatRooms
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
