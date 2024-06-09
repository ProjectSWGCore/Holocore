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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.location.Point3D
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import me.joshlarson.jlcommon.log.Log
import me.joshlarson.json.JSON
import me.joshlarson.json.JSONArray
import me.joshlarson.json.JSONException
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

class BuildingCellLoader internal constructor() : DataLoader() {
	private val buildingMap: MutableMap<String, List<CellInfo>> = HashMap()

	fun getBuilding(buildingIff: String): List<CellInfo>? {
		return buildingMap[buildingIff]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/objects/building_cells.sdb")).use { set ->
			buildingMap.putAll(set.stream { CellInfo(it) }.collect(Collectors.groupingBy { obj: CellInfo -> obj.building }))
		}
	}

	class CellInfo(set: SdbResultSet) {
		val building: String = set.getText(0)
		val id: Int = set.getInt(1).toInt()
		val name: String = set.getText(2).intern()
		private val _neighbors: MutableList<PortalInfo> = ArrayList()
		val neighbors: List<PortalInfo>
			get() = Collections.unmodifiableList(_neighbors)

		init {
			try {
				val parts = JSON.readArray(set.getText(3))
				for (i in parts.indices) {
					val neighbor = JSONArray(parts.getArray(i))
					val min = JSONArray(neighbor.getArray(1))
					val max = JSONArray(neighbor.getArray(2))
					val p1 = Point3D(min.getDouble(0), min.getDouble(1), min.getDouble(2))
					val p2 = Point3D(max.getDouble(0), min.getDouble(1), max.getDouble(2))
					_neighbors.add(PortalInfo(id, neighbor.getInt(0), p1, p2, max.getDouble(1) - min.getDouble(1)))
				}
			} catch (e: IOException) {
				Log.w("Invalid cell info: %s", set.getText(3))
			} catch (e: JSONException) {
				Log.w("Invalid cell info: %s", set.getText(3))
			}
		}
	}

	class PortalInfo(val cell1: Int, val cell2: Int, val frame1: Point3D, val frame2: Point3D, val height: Double) {
		fun getOtherCell(cell: Int): Int {
			assert(cell1 == cell || cell2 == cell)
			return if (cell1 == cell) cell2 else cell1
		}
	}
}
