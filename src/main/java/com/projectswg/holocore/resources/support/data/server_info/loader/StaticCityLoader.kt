/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import java.io.File

class StaticCityLoader : DataLoader() {
	private val cities = mutableMapOf<Terrain, MutableList<City>>()

	fun getCities(terrain: Terrain): Collection<City> {
		return cities.getOrElse(terrain) { emptyList() }
	}

	override fun load() {
		SdbLoader.load(File("serverdata/map/cities.sdb")).use { set ->
			while (set.next()) {
				val t = Terrain.getTerrainFromName(set.getText("terrain"))
				val list: MutableList<City> = cities.computeIfAbsent(t) { mutableListOf() }
				list.add(City(set.getText("city"), set.getInt("x").toInt(), set.getInt("z").toInt(), set.getInt("radius").toInt()))
			}
		}
	}

	class City(val name: String, private val x: Int, private val z: Int, private val radius: Int) {

		fun isWithinRange(obj: SWGObject): Boolean {
			return square(obj.x.toInt() - x) + square(obj.z.toInt() - z) <= square(radius)
		}

		override fun toString(): String {
			return String.format("City[%s, (%d, %d), radius=%d]", name, x, z, radius)
		}

		private fun square(x: Int): Int {
			return x * x
		}
	}
}