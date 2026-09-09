/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.services.support.objects

import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap

class BuildoutObjectStorage : Service() {

	private val testObjects = ConcurrentHashMap<Long, SWGObject>()

	override fun initialize(): Boolean {
		val buildouts = DataLoader.buildouts(mutableListOf())
		val objects = buildouts.objects
		ObjectStorageService.ObjectLookup.setObjectAuthority { key: Long -> objects[key] ?: testObjects[key] }
		ObjectStorageService.BuildingLookup.setBuildingAuthority(buildouts.buildings::get)
		return true
	}

	override fun terminate(): Boolean {
		ObjectStorageService.ObjectLookup.setObjectAuthority(null)
		ObjectStorageService.BuildingLookup.setBuildingAuthority(null)
		return true
	}

	@IntentHandler
	private fun processObjectCreatedIntent(intent: ObjectCreatedIntent) {
		val obj = intent.obj
		val replaced = testObjects.put(obj.objectId, obj)
		if (replaced != null && replaced !== obj) throw IllegalStateException("Replaced object in object map! Old: $replaced  New: $obj")
	}
}
