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

package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

class StructureInfoLoader : DataLoader() {
	
	private var structureInfo: Map<String, StructureInfo> = HashMap()
	
	val structures: Map<String, StructureInfo>
		get() = Collections.unmodifiableMap(structureInfo)
	
	fun getStructureInfo(structureTemplate: String): StructureInfo? {
		return structureInfo[structureTemplate]
	}
	
	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/housing/housing_datatable.sdb")).use { set ->
			structureInfo = set.stream { StructureInfo(it) }.collect(Collectors.toMap({ it.structureTemplate }, { it }))
		}
	}
	
	class StructureInfo(set: SdbResultSet) {
		
		val structureTemplate = set.getText("structure")
		val deedTemplate = set.getText("deed")
		val constructionTemplate = set.getText("deed")
		val footprintTemplate = set.getText("footprint_template")
		val signTemplate = set.getText("sign_template")
		val lotsNeeded = set.getInt("lots_needed")
		val ejectRange = set.getInt("eject_range")
		val sign = set.getBoolean("sign")
		val signX = set.getReal("sign_x")
		val signY = set.getReal("sign_y")
		val signZ = set.getReal("sign_z")
		val signHeading = set.getReal("sign_heading")
		val signAltX = set.getReal("sign_alt_x")
		val signAltY = set.getReal("sign_alt_y")
		val signAltZ = set.getReal("sign_alt_z")
		val signAltHeading = set.getReal("sign_alt_heading")
		val civic = set.getBoolean("civic")
		val cityRank = set.getInt("city_rank")
		val cityCost = set.getInt("city_cost")
		val type = set.getText("type")
		val shuttleport = set.getBoolean("shuttleport")
		val cloning = set.getBoolean("cloning")
		val garage = set.getBoolean("garage")
		val reclaim = set.getBoolean("reclaim")
		val maintenanceRage = set.getInt("maintenance_rate")
		val decayRate = set.getInt("decay_rate")
		val condition = set.getInt("condition")
		val costRedeed = set.getInt("cost_redeed")
		val powerRate = set.getInt("power_rate")
		val hopperMin = set.getInt("hopper_min")
		val hopperMax = set.getInt("hopper_max")
		val skillmod = set.getText("skillmod")
		val skillmodValue = set.getInt("skillmod_value")
		val skillmodMessage = set.getText("skillmod_message")
		
	}
	
}
