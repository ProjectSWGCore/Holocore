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

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class NoSpawnZoneLoader : DataLoader() {
	private val noSpawnZoneMap: MutableMap<Terrain, MutableCollection<NoSpawnZoneInfo>> = EnumMap(Terrain::class.java)

	/**
	 * Determines whether a given location is located within a zone where spawning buildings and dynamic NPCs is disallowed.
	 * @param location to check
	 * @return `true` if the location is located inside a no spawn zone and `false` otherwise
	 */
	fun isInNoSpawnZone(location: Location): Boolean {
		val terrain = location.terrain

		val noSpawnZoneInfos = getNoSpawnZoneInfos(terrain)

		for (noSpawnZoneInfo in noSpawnZoneInfos) {
			val x = noSpawnZoneInfo.x
			val z = noSpawnZoneInfo.z
			val radius = noSpawnZoneInfo.radius

			val noSpawnLocation = Location.builder().setTerrain(terrain).setX(x.toDouble()).setZ(z.toDouble()).build()

			val noSpawnZone = noSpawnLocation.isWithinFlatDistance(location, radius.toDouble())

			if (noSpawnZone) {
				return true
			}
		}

		return false
	}

	/**
	 * Finds no spawn zone information for the given terrain.
	 * Dynamic NPCs and player structures must not be spawned in these zones!
	 * @param terrain to find no spawn zone information for
	 * @return never `null`
	 */
	fun getNoSpawnZoneInfos(terrain: Terrain?): Collection<NoSpawnZoneInfo> {
		return noSpawnZoneMap.getOrDefault(terrain, emptyList())
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/spawn/zones_nobuild_nospawn.sdb")).use { set ->
			while (set.next()) {
				val planet = set.getText("terrain")
				val terrain = checkNotNull(Terrain.getTerrainFromName(planet)) { "unable to find terrain by name $planet" }
				val noSpawnInfos = noSpawnZoneMap.computeIfAbsent(terrain) { ArrayList() }
				val info = NoSpawnZoneInfo(set)
				noSpawnInfos.add(info)
			}
		}
	}

	class NoSpawnZoneInfo(set: SdbResultSet) {
		val x: Long = set.getInt("x")
		val z: Long = set.getInt("z")
		val radius: Long = set.getInt("radius")
	}
}
