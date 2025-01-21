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
package com.projectswg.holocore.services.support.npc.ai

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import com.projectswg.holocore.resources.support.npc.ai.dynamic.DynamicMovementObject
import com.projectswg.holocore.resources.support.npc.ai.dynamic.DynamicMovementProcessor
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class AIDynamicMovementService : Service() {

	private val coroutineScope = HolocoreCoroutine.childScope()
	private val dynamicObjects = EnumMap<Terrain, CopyOnWriteArrayList<DynamicMovementObject>>(Terrain::class.java)
	
	init {
		// Guarantee all terrains are set so that we don't have to worry about concurrent access later
		for (terrain in Terrain.entries) {
			dynamicObjects[terrain] = CopyOnWriteArrayList()
		}
	}
	
	override fun start(): Boolean {
		val spawnsPerPlanet = config.getInt(this, "spawnsPerPlanet", 10)
		for (terrain in listOf(Terrain.TATOOINE)) {
			launchDynamicMovementObjectHandler(terrain, "DynamicObject-${terrain.name}-dev")
			repeat(spawnsPerPlanet) {
				launchDynamicMovementObjectHandler(terrain, "DynamicObject-${terrain.name}-$it")
			}
		}
		return super.start()
	}

	override fun stop(): Boolean {
		coroutineScope.cancelAndWait()
		return super.stop()
	}
	
	private fun launchDynamicMovementObjectHandler(terrain: Terrain, objectName: String) {
		coroutineScope.launch {
			try {
				while (isActive) {
					delay(5_000L)
					val spawnLocation = if (objectName.endsWith("-dev"))
						Location.builder().setTerrain(terrain).setX(1024.0).setZ(1024.0).setY(ServerData.terrains.getHeight(terrain, 1024.0, 1024.0)).build()
					else
						DynamicMovementProcessor.createSpawnLocation(terrain) ?: continue
					Log.d("Created dynamic movement object: %s", spawnLocation)
					val dynamicObject = DynamicMovementObject(spawnLocation, objectName)
					dynamicObject.launch()
					dynamicObjects[terrain]!!.add(dynamicObject)
					try {
						handleObjectLoop(dynamicObject)
					} finally {
						dynamicObject.destroy()
						dynamicObjects[terrain]!!.remove(dynamicObject)
					}
				}
			} finally {

			}
		}
	}
	
	private suspend fun CoroutineScope.handleObjectLoop(dynamicObject: DynamicMovementObject) {
		while (isActive) {
			dynamicObject.act()
			delay(10_000L)
		}
	}

}