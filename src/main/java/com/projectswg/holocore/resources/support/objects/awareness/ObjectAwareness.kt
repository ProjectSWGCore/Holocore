/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects.awareness

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import com.projectswg.holocore.utilities.launchWithFixedRate
import kotlinx.coroutines.CoroutineScope

class ObjectAwareness(private val updateRateMilliseconds: Long = 100L) {
	private val terrains: Array<TerrainMap> = Array(Terrain.entries.size) { TerrainMap() }
	private var coroutineScope: CoroutineScope? = null

	fun startThreadPool() {
		coroutineScope = HolocoreCoroutine.childScope().let { scope ->
			for (terrain in terrains) {
				scope.launchWithFixedRate(updateRateMilliseconds) { terrain.updateChunks() }
			}
			return@let scope
		}
	}

	fun stopThreadPool() {
		coroutineScope?.cancelAndWait()
	}

	/**
	 * Called when an object was created
	 *
	 * @param obj the object created
	 */
	fun createObject(obj: SWGObject) {
		terrains[obj.terrain.ordinal].add(obj)
	}

	/**
	 * Called when an object is destroyed
	 *
	 * @param obj the object destroyed
	 */
	fun destroyObject(obj: SWGObject) {
		terrains[obj.terrain.ordinal].remove(obj)
	}

	/**
	 * Called when an object needs an update
	 *
	 * @param obj the object to update
	 */
	fun updateObject(obj: SWGObject) {
		terrains[obj.terrain.ordinal].move(obj)
	}

	/**
	 * Updates all affected chunks
	 */
	fun updateChunks() {
		for (terrain in terrains) terrain.updateChunks()
	}
}
