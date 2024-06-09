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

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException
import java.util.*

class SkillLoader internal constructor() : DataLoader() {
	private val skillsByName: MutableMap<String, SkillInfo> = HashMap()

	fun getSkillByName(name: String): SkillInfo? {
		return skillsByName[name]
	}

	val skills: Collection<SkillInfo>
		get() = Collections.unmodifiableCollection(skillsByName.values)

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/skill/skills.sdb")).use { set ->
			while (set.next()) {
				val skills = SkillInfo(set)
				skillsByName[skills.name] = skills
			}
		}
	}

	class SkillInfo(set: SdbResultSet) {
		val name: String = set.getText("name")
		val parent: String = set.getText("parent")
		val graphType: String = set.getText("graph_type")
		val isGodOnly: Boolean = set.getBoolean("god_only")
		val isTitle: Boolean = set.getBoolean("is_title")
		val isProfession: Boolean = set.getBoolean("is_profession")
		val isHidden: Boolean = set.getBoolean("is_hidden")
		val moneyRequired: Int = set.getInt("money_required").toInt()
		val pointsRequired: Int = set.getInt("points_required").toInt()
		val skillsRequiredCount: Int = set.getInt("skills_required_count").toInt()
		val skillsRequired: List<String> = splitCsv(set.getText("skills_required"))
		val preclusionSkills: List<String> = splitCsv(set.getText("preclusion_skills"))
		val xpType: String = set.getText("xp_type")
		val xpCost: Int = set.getInt("xp_cost").toInt()
		val xpCap: Int = set.getInt("xp_cap").toInt()
		val missionsRequired: List<String> = splitCsv(set.getText("missions_required"))
		val apprenticeshipsRequired: Int = set.getInt("apprenticeships_required").toInt()
		private val statsRequired: List<String> = splitCsv(set.getText("stats_required"))
		private val speciesRequired: List<String> = splitCsv(set.getText("species_required"))
		val jediStateRequired: String = set.getText("jedi_state_required")
		val skillAbility: String = set.getText("skill_ability")
		val commands: List<String> = splitCsv(set.getText("commands"))
		val skillMods: Map<String, Int> = createSkillModMap(splitCsv(set.getText("skill_mods")))
		val schematicsGranted: List<String> = splitCsv(set.getText("schematics_granted"))
		val schematicsRevoked: List<String> = splitCsv(set.getText("schematics_revoked"))
		val isSearchable: Boolean = set.getBoolean("searchable")
		val ender: Int = set.getInt("ender").toInt()

		companion object {
			private fun splitCsv(str: String): List<String> {
				if (str.isEmpty()) return listOf()
				else if (str.indexOf(',') == -1) return listOf(str)
				return str.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toList()
			}

			private fun createSkillModMap(elements: List<String>): Map<String, Int> {
				val skillMods: MutableMap<String, Int> = HashMap()
				for (element in elements) {
					val split = element.split("=".toRegex(), limit = 2).toTypedArray()
					if (split.size < 2) continue
					skillMods[split[0]] = split[1].toInt()
				}
				return skillMods
			}
		}
	}
}
