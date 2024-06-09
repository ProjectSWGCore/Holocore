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
package com.projectswg.holocore.resources.support.data.server_info.loader.npc

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.data.server_info.loader.*
import java.io.File
import java.io.IOException
import java.util.function.Consumer
import java.util.stream.Collectors

class NpcPatrolRouteLoader : DataLoader() {
	private val patrolRouteMap: MutableMap<String, List<PatrolRouteWaypoint>> = HashMap()

	operator fun get(groupId: String): List<PatrolRouteWaypoint> {
		return patrolRouteMap[groupId]!!
	}

	fun forEach(c: Consumer<List<PatrolRouteWaypoint>>?) {
		patrolRouteMap.values.forEach(c)
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/patrol/patrol_id.msdb")).use { set ->
			patrolRouteMap.putAll(set.stream { PatrolRouteWaypoint(it) }.collect(Collectors.groupingBy { obj: PatrolRouteWaypoint -> obj.groupId }))
		}
	}

	class PatrolRouteWaypoint(set: SdbResultSet) {
		val groupId: String = set.getText("patrol_group")
		val patrolId: String = set.getText("patrol_id")
		val patrolType: PatrolType = parsePatrolType(set.getText("patrol_type"))
		val terrain: Terrain = Terrain.valueOf(set.getText("terrain"))
		val buildingId: String = set.getText("building_id")
		val cellId: Int = set.getInt("cell_id").toInt()
		val x: Double = set.getReal("x")
		val y: Double = set.getReal("y")
		val z: Double = set.getReal("z")
		val delay: Double = set.getReal("pause")

		companion object {
			private fun parsePatrolType(str: String): PatrolType {
				return when (str.uppercase()) {
					"FLIP" -> PatrolType.FLIP
					"LOOP" -> PatrolType.LOOP
					else   -> PatrolType.LOOP
				}
			}
		}
	}

	enum class PatrolType {
		LOOP,
		FLIP
	}
}
