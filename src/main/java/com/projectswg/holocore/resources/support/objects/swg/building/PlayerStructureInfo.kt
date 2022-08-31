/***********************************************************************************
 * Copyright (c) 2022 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.swg.building

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup

class PlayerStructureInfo(owner: CreatureObject?) : MongoPersistable {
	
	private var ownerId: Long = 0L
	
	var owner: CreatureObject? = owner
		get() {
			if (field == null && ownerId != 0L)
				field = ObjectLookup.getObjectById(ownerId) as CreatureObject
			return field
		}
		set(value) {
			field = value
			ownerId = value?.objectId ?: 0L
		}
	
	override fun readMongo(data: MongoData) {
		ownerId = data.getLong("ownerId", 0)
	}
	
	override fun saveMongo(data: MongoData) {
		data.putLong("ownerId", ownerId)
	}
}