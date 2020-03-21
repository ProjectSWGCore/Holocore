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

package com.projectswg.holocore.resources.support.data.server_info.database

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import me.joshlarson.jlcommon.log.Log
import java.sql.DriverManager
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class Database(mongo: MongoDatabase) {
	
	val config = DatabaseTable(mongo.getCollection("config"), null, null, null)
	private val configuration = config.mongo.find(Filters.exists("connector")).map { DatabaseConfiguration(it) }.first()
	private val connection = if (configuration == null || configuration.connector != "mariadb") null
							else DriverManager.getConnection("jdbc:mariadb://${configuration.host}:${configuration.port}/${configuration.database}?autoReconnect=true&user=${configuration.user}&password=${configuration.pass}")
	val users = DatabaseTable(mongo.getCollection("users"), configuration, connection, configuration?.tables?.get("users"))
	val objects = DatabaseTable(mongo.getCollection("objects"), configuration, connection, configuration?.tables?.get("objects"))
	val resources = DatabaseTable(mongo.getCollection("resources"), configuration, connection, configuration?.tables?.get("resources"))
	val gcwRegions = DatabaseTable(mongo.getCollection("gcwregions"), configuration, connection, configuration?.tables?.get("gcwRegions"))
	
	init {
		setupMongoLogging()
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
