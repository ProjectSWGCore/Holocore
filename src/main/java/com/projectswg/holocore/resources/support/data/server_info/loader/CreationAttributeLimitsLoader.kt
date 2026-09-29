/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
import java.io.IOException

class CreationAttributeLimitsLoader internal constructor() : DataLoader() {
	private val limitsByRace: MutableMap<Race, AttributeLimits> = HashMap()

	fun getLimits(race: Race): AttributeLimits? = limitsByRace[race]

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/creation/attribute_limits.sdb")).use { set ->
			while (set.next()) {
				val limits = AttributeLimits(set)
				limitsByRace[Race.getRace(set.getText("male_template"))] = limits
				limitsByRace[Race.getRace(set.getText("female_template"))] = limits
			}
		}
	}

	class AttributeLimits(set: SdbResultSet) {
		val minHealth: Int = set.getInt("min_health").toInt()
		val maxHealth: Int = set.getInt("max_health").toInt()
		val minConstitution: Int = set.getInt("min_constitution").toInt()
		val maxConstitution: Int = set.getInt("max_constitution").toInt()
		val minAction: Int = set.getInt("min_action").toInt()
		val maxAction: Int = set.getInt("max_action").toInt()
		val minStamina: Int = set.getInt("min_stamina").toInt()
		val maxStamina: Int = set.getInt("max_stamina").toInt()
		val minMind: Int = set.getInt("min_mind").toInt()
		val maxMind: Int = set.getInt("max_mind").toInt()
		val minWillpower: Int = set.getInt("min_willpower").toInt()
		val maxWillpower: Int = set.getInt("max_willpower").toInt()
	}
}
