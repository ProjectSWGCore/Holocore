/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException
import java.util.*

class TravelCostLoader internal constructor() : DataLoader() {
	private val costs: MutableMap<Terrain, Map<Terrain, Int>> = EnumMap(Terrain::class.java)

	fun isCostDefined(source: Terrain): Boolean {
		return costs.containsKey(source)
	}

	fun getCost(source: Terrain, destination: Terrain): Int {
		val costMap = costs[source] ?: return 0
		return costMap.getOrDefault(destination, 0)
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/travel/travel_costs.sdb")).use { set ->
			while (set.next()) {
				val travel = TravelCostInfo(set)
				costs[travel.planet] = travel.getCosts()
			}
		}
		for (key in costs.keys) {
			for (costMap in costs.values) {
				assert(costMap.keys == costs.keys) { "planet $key is improperly defined in travel_costs.sdb" }
			}
		}
	}

	class TravelCostInfo(set: SdbResultSet) {
		val planet: Terrain = Terrain.getTerrainFromName(set.getText("planet"))
		private val costs = EnumMap<Terrain, Int>(Terrain::class.java)

		init {
			for (col in set.columns) {
				if (col.equals("planet", ignoreCase = true)) continue
				costs[Terrain.getTerrainFromName(col)] = set.getInt(col).toInt()
			}
		}

		fun getCosts(): Map<Terrain, Int> {
			return Collections.unmodifiableMap(costs)
		}
	}
}
