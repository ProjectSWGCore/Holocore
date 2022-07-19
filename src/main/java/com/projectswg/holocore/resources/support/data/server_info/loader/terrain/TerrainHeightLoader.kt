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

package com.projectswg.holocore.resources.support.data.server_info.loader.terrain

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.data.swgiff.parsers.SWGParser
import com.projectswg.common.data.swgiff.parsers.terrain.TerrainTemplate
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import java.util.EnumMap

class TerrainHeightLoader : DataLoader() {
	
	private val terrains = EnumMap<Terrain, TerrainTemplate>(Terrain::class.java)
	
	override fun load() {
		for (terrain in TERRAIN_LIST) {
			terrains[terrain] = SWGParser.parse(terrain.file)
		}
	}
	
	fun getTerrain(terrain: Terrain): TerrainTemplate? {
		return terrains[terrain]
	}
	
	fun getHeight(l: Location.LocationBuilder): Double = getHeight(l.terrain, l.x, l.z)
	fun getHeight(l: Location): Double = getHeight(l.terrain, l.x, l.z)
	
	fun getHeight(terrain: Terrain, x: Double, z: Double): Double {
		return terrains[terrain]?.getHeight(x.toFloat(), z.toFloat())?.height?.toDouble() ?: 0.0
	}
	
	companion object {
		
		private val TERRAIN_LIST = listOf(
			Terrain.CORELLIA,
			Terrain.DANTOOINE,
			Terrain.DATHOMIR,
			Terrain.ENDOR,
			Terrain.KASHYYYK_DEAD_FOREST,
			Terrain.KASHYYYK_HUNTING,
			Terrain.KASHYYYK_MAIN,
			Terrain.KASHYYYK_NORTH_DUNGEONS,
			Terrain.KASHYYYK_POB_DUNGEONS,
			Terrain.KASHYYYK_RRYATT_TRAIL,
			Terrain.KASHYYYK_SOUTH_DUNGEONS,
			Terrain.KASHYYYK,
			Terrain.LOK,
			Terrain.MUSTAFAR,
			Terrain.NABOO,
			Terrain.RORI,
			Terrain.TAANAB,
			Terrain.TALUS,
			Terrain.TATOOINE,
			Terrain.UMBRA,
			Terrain.YAVIN4,
		)
		
	}
	
}