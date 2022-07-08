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
		val key: String
		val raceMap = HashMap<Race, Boolean>()

		init {
			key = set.getText("object template name")
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