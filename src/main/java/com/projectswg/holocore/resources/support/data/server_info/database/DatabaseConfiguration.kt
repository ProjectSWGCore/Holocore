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

import org.bson.Document


class DatabaseConfiguration(doc: Document) {
	
	val connector: String = doc.getString("connector") ?: "mongodb"
	val host: String = doc.getString("host") ?: ""
	val port: Int = doc.getInteger("port", 0)
	val database: String = doc.getString("database") ?: ""
	val user: String = doc.getString("user") ?: ""
	val pass: String = doc.getString("pass") ?: ""
	val accessLevels = doc.getList("accessLevels", Document::class.java, ArrayList())
			.map { ConfigurationAccessLevels(it) }
			.filter { it.user.isNotEmpty() }
			.groupBy { it.user }
			.mapValues { it.value.last().accessLevel }
	val tables: Map<String, String> = doc.get("tables", Document()).toMap().filter { it.key is String && it.value is String }.mapValues { it.value as String }
	
	class ConfigurationAccessLevels(doc: Document) {
		val user: String = doc.getString("user") ?: ""
		val accessLevel: String = doc.getString("accessLevel") ?: "player"
	}
	
}
