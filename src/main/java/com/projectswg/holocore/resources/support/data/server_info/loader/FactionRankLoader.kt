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

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException
import java.util.*

class FactionRankLoader internal constructor() : DataLoader() {
	private val _ranks: MutableList<FactionRankInfo> = ArrayList()
	val ranks: List<FactionRankInfo>
		get() = Collections.unmodifiableList(_ranks)

	fun getRank(index: Int): FactionRankInfo? = _ranks.getOrNull(index)

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/faction/rank.sdb")).use { set ->
			while (set.next()) {
				_ranks.add(FactionRankInfo(set))
			}
		}
	}

	class FactionRankInfo(set: SdbResultSet) {
		val index: Int = set.getInt("index").toInt()
		val name: String = set.getText("name")
		val cost: Int = set.getInt("cost").toInt()
		val delegateRatioFrom: Int = set.getInt("delegate_ratio_from").toInt()
		val delegateRatioTo: Int = set.getInt("delegate_ratio_to").toInt()
	}
}
