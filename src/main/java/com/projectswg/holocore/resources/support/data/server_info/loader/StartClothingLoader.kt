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
import java.io.File
import java.io.IOException
import java.util.*

class StartClothingLoader internal constructor() : DataLoader() {
	private val clothing: MutableMap<Race, Map<String, List<String>>> = EnumMap(Race::class.java)

	fun getClothing(race: Race, clothingName: String): List<String>? {
		val raceClothing = clothing[race] ?: return null
		return raceClothing[clothingName]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/player/start_clothing.sdb")).use { set ->
			val columns = set.columns
			while (set.next()) {
				val race = Race.getRace(set.getText("race"))
				val clothingItems: MutableMap<String, List<String>> = HashMap()
				for (clothingName in columns) {
					if (clothingName == "race") continue
					val clothingArray = set.getText(clothingName)
					if (clothingArray.isBlank()) continue

					clothingItems[clothingName] = listOf(*clothingArray.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
				}
				clothing[race] = Collections.unmodifiableMap(clothingItems)
			}
		}
	}
}
