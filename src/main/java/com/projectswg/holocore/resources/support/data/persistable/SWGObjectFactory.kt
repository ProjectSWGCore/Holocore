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
package com.projectswg.holocore.resources.support.data.persistable

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

object SWGObjectFactory {
	@JvmOverloads
	fun save(obj: SWGObject, data: MongoData = MongoData()): MongoData {
		obj.saveMongo(data)
		assert(data.containsKey("id")) { "serialized MongoData does not contain the objectId" }
		assert(data.containsKey("parent")) { "serialized MongoData does not contain the parent id" }
		assert(data.containsKey("parentCell")) { "serialized MongoData does not contain the parent cell number" }
		assert(data.containsKey("template")) { "serialized MongoData does not contain the template" }
		return data
	}

	fun create(data: MongoData): SWGObject {
		val objectId = data.getLong("id", 0)
		val template = data.getString("template")
		assert(objectId != 0L) { "objectId is not defined in MongoData" }
		checkNotNull(template) { "template is not defined in MongoData" }
		val obj = ObjectCreator.createObjectFromTemplate(objectId, template)
		obj.readMongo(data)
		return obj
	}
}
