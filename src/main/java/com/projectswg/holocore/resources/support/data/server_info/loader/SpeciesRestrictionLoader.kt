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

import com.projectswg.common.data.encodables.tangible.Race
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File

class SpeciesRestrictionLoader : DataLoader() {

	private val templateMap = HashMap<String, TemplateInfo>()

	fun isAllowedToWear(template: String, race: Race): Boolean {
		val key = template.substringAfterLast("/").replace(".iff", "")
		val templateInfo = templateMap[key]
		templateInfo ?: return false

		return templateInfo.raceMap.getOrDefault(race, false)
	}

	override fun load() {
		val set = SdbLoader.load(File("serverdata/appearance/appearance_table.sdb"))

		set.use {
			while (set.next()) {
				val templateInfo = TemplateInfo(set)

				templateMap[templateInfo.key] = templateInfo
			}
		}
	}

	private class TemplateInfo(set: SdbResultSet) {
		val key: String = set.getText("object template name")
		val raceMap = HashMap<Race, Boolean>()

		init {
			raceMap[Race.HUMAN_MALE] = isAllowed(set, "male human")
			raceMap[Race.HUMAN_FEMALE] = isAllowed(set, "female human")
			raceMap[Race.RODIAN_MALE] = isAllowed(set, "male rodian")
			raceMap[Race.RODIAN_FEMALE] = isAllowed(set, "female rodian")
			raceMap[Race.MONCAL_MALE] = isAllowed(set, "male moncal")
			raceMap[Race.MONCAL_FEMALE] = isAllowed(set, "female moncal")
			raceMap[Race.WOOKIEE_MALE] = isAllowed(set, "male wookiee")
			raceMap[Race.WOOKIEE_FEMALE] = isAllowed(set, "female wookiee")
			raceMap[Race.TWILEK_MALE] = isAllowed(set, "male twi'lek")
			raceMap[Race.TWILEK_FEMALE] = isAllowed(set, "female twi'lek")
			raceMap[Race.TRANDOSHAN_MALE] = isAllowed(set, "male trandoshan")
			raceMap[Race.TRANDOSHAN_FEMALE] = isAllowed(set, "female trandoshan")
			raceMap[Race.ZABRAK_MALE] = isAllowed(set, "male zabrak")
			raceMap[Race.ZABRAK_FEMALE] = isAllowed(set, "female zabrak")
			raceMap[Race.BOTHAN_MALE] = isAllowed(set, "male bothan")
			raceMap[Race.BOTHAN_FEMALE] = isAllowed(set, "female bothan")
			raceMap[Race.ITHORIAN_MALE] = isAllowed(set, "male ithorian")
			raceMap[Race.ITHORIAN_FEMALE] = isAllowed(set, "female ithorian")
			raceMap[Race.SULLUSTAN_MALE] = isAllowed(set, "male sullustan")
			raceMap[Race.SULLUSTAN_FEMALE] = isAllowed(set, "female sullustan")
		}

		private fun isAllowed(set: SdbResultSet, type: String) = set.getText(type) != "x"
	}

}