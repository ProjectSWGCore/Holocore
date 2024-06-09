/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.crafting.resource.raw

import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource.RawResourceBuilder
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.IOException

class RawResourceLoader : DataLoader() {
	private val resources: MutableMap<Long, RawResource?> = HashMap()

	fun getResources(): List<RawResource?> {
		return ArrayList(resources.values)
	}

	fun getResource(id: Long): RawResource? {
		return resources[id]
	}

	@Throws(IOException::class)
	override fun load() {
		resources.clear()
		val startTime = StandardLog.onStartLoad("raw resources")
		try {
			SdbLoader.load(File("serverdata/resources/resources.sdb")).use { set ->
				while (set.next()) {
					val crateTemplate = StringBuilder(set.getText("crate_template"))
					crateTemplate.insert(crateTemplate.lastIndexOf("/") + 1, "resource_container_")
					val resource = RawResourceBuilder(set.getInt("id")).setParent(resources[set.getInt("parent")])
						.setName(set.getText("resource_name"))
						.setCrateTemplate(crateTemplate.toString())
						.setMinPools(set.getInt("min_pools").toInt())
						.setMaxPools(set.getInt("max_pools").toInt())
						.setMinTypes(set.getInt("min_types").toInt())
						.setMaxTypes(set.getInt("max_types").toInt())
						.setAttrColdResistance(set.getInt("attr_cold_resist") != 0L)
						.setAttrConductivity(set.getInt("attr_conductivity") != 0L)
						.setAttrDecayResistance(set.getInt("attr_decay_resist") != 0L)
						.setAttrEntangleResistance(set.getInt("attr_entangle_resist") != 0L)
						.setAttrFlavor(set.getInt("attr_flavor") != 0L)
						.setAttrHeatResistance(set.getInt("attr_heat_resist") != 0L)
						.setAttrMalleability(set.getInt("attr_malleability") != 0L)
						.setAttrOverallQuality(set.getInt("attr_quality") != 0L)
						.setAttrPotentialEnergy(set.getInt("attr_potential_energy") != 0L)
						.setAttrShockResistance(set.getInt("attr_shock_resist") != 0L)
						.setRecycled(set.getInt("recycled") != 0L)
						.build()
					resources[resource.id] = resource
				}
			}
		} catch (e: IOException) {
			Log.e(e)
		}
		StandardLog.onEndLoad(resources.size, "raw resources", startTime)
	}
}
