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

import com.projectswg.common.data.encodables.tangible.PvpFaction
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import me.joshlarson.jlcommon.log.Log
import java.io.File

class CloningFacilityLoader : DataLoader() {
	private val facilityDataMap = mutableMapOf<String, FacilityData>()

	fun getFacility(objectTemplate: String): FacilityData? {
		return facilityDataMap[objectTemplate]
	}

	override fun load() {
		SdbLoader.load(File("serverdata/cloning/cloning_respawn.sdb")).use { set ->
			while (set.next()) {
				val tubes = tubes(set)
				val stfCellValue = set.getText("stf_name")
				val stfName = if (stfCellValue == "-") null else stfCellValue
				val factionRestriction = pvpFaction(stfCellValue)
				val facilityType = FacilityType.valueOf(set.getText("clone_type"))
				val facilityData = FacilityData(
					factionRestriction,
					set.getReal("x"),
					set.getReal("y"),
					set.getReal("z"),
					set.getText("cell"),
					facilityType,
					stfName,
					set.getInt("heading").toInt(),
					tubes
				)
				val objectTemplate = set.getText("structure")
				val sharedObjectTemplate = ClientFactory.formatToSharedFile(objectTemplate)
				if (facilityDataMap.put(sharedObjectTemplate, facilityData) != null) {
					// Duplicates are not allowed!
					Log.e("Duplicate entry for %s in row %d. Replacing previous entry with new", objectTemplate, set.line)
				}
			}
		}
	}

	private fun pvpFaction(stfCellValue: String?): PvpFaction? {
		return when (stfCellValue) {
			"FACTION_REBEL"    -> PvpFaction.REBEL
			"FACTION_IMPERIAL" -> PvpFaction.IMPERIAL
			else               -> null
		}
	}

	private fun tubes(set: SdbLoader.SdbResultSet): Collection<TubeData> {
		val tubes = mutableListOf<TubeData>()
		val tubeCount = set.getInt("tubes")
		for (i in 1..tubeCount) {
			val tubeName = "tube$i"
			val tubeData = TubeData(set.getReal(tubeName + "_x"), set.getReal(tubeName + "_z"), set.getReal(tubeName + "_heading"))
			
			tubes += tubeData
		}
		return tubes
	}
}

enum class FacilityType {
	STANDARD, RESTRICTED, PLAYER_CITY, CAMP, PRIVATE_INSTANCE, FACTION_IMPERIAL, FACTION_REBEL, PVP_REGION_ADVANCED_IMPERIAL, PVP_REGION_ADVANCED_REBEL
}

data class FacilityData(val factionRestriction: PvpFaction?, val x: Double, val y: Double, val z: Double, val cell: String, val facilityType: FacilityType, val stfName: String?, val heading: Int, val tubes: Collection<TubeData>)
data class TubeData(val tubeX: Double, val tubeZ: Double, val tubeHeading: Double)
