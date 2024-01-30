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

import com.projectswg.common.data.CRC
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbTextColumnArraySet
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException

class PerformanceLoader internal constructor() : DataLoader() {
	private val nameMap = mutableMapOf<String, PerformanceInfo>()
	private val danceMap = mutableMapOf<Int, PerformanceInfo>()

	fun getPerformanceByName(performanceName: String): PerformanceInfo? {
		return nameMap[performanceName]
	}

	fun getPerformanceByDanceId(danceVisualId: Int): PerformanceInfo? {
		return danceMap[danceVisualId]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/performance/performance.sdb")).use { set ->
			val flourishes = set.getTextArrayParser("flourish(%d+)", null)
			while (set.next()) {
				val performance = PerformanceInfo(set, flourishes)
				nameMap[performance.performanceName] = performance
				danceMap[performance.danceVisualId] = performance
			}
		}
	}

	class PerformanceInfo(set: SdbResultSet, flourishes: SdbTextColumnArraySet) {
		val performanceName: String = set.getText("performance_name")
		val instrumentAudioId: Int = set.getInt("instrument_audio_id").toInt()
		val requiredSong: String = set.getText("required_song")
		val requiredInstrument: String = set.getText("required_instrument")
		val requiredDance: String = set.getText("required_dance")
		val danceVisualId: Int = set.getInt("dance_visual_id").toInt()
		val actionPointsPerLoop: Int = set.getInt("action_points_per_loop").toInt()
		val loopDuration: Double = set.getReal("loop_duration")
		val type: CRC = CRC(set.getInt("type").toInt())
		val baseXp: Int = set.getInt("base_xp").toInt()
		val flourishXpMod: Int = set.getInt("flourish_xp_mod").toInt()
		val healMindWound: Int = set.getInt("heal_mind_wound").toInt()
		val healShockWound: Int = set.getInt("heal_shock_wound").toInt()
		val requiredSkillMod: String = set.getText("required_skill_mod")
		val requiredSkillModValue: Int = set.getInt("required_skill_mod_value").toInt()
		val mainloop: String = set.getText("mainloop")
		val flourishes = flourishes.getArray(set).toList()
		val intro: String = set.getText("intro")
		val outro: String = set.getText("outro")
	}
}
