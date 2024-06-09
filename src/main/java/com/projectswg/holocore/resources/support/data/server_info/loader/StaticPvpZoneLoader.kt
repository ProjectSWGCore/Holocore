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

class StaticPvpZoneLoader : DataLoader() {
	private val _staticPvpZones: MutableCollection<StaticPvpZoneInfo> = ArrayList()
	val staticPvpZones: Collection<StaticPvpZoneInfo>
		get() = Collections.unmodifiableCollection(_staticPvpZones)

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/gcw/pvp_zones.sdb")).use { set ->
			while (set.next()) {
				_staticPvpZones.add(StaticPvpZoneInfo(set))
			}
		}
	}

	class StaticPvpZoneInfo(set: SdbResultSet) {
		val id: Int = set.getInt("pvp_zone_id").toInt()
		val location: Location = Location.builder().setX(set.getInt("x").toDouble()).setY(0.0).setZ(set.getInt("z").toDouble()).setTerrain(Terrain.getTerrainFromName(set.getText("terrain"))).build()
		val radius: Double = set.getInt("radius").toDouble()
	}
}
