/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import java.io.File
import java.io.IOException
import java.util.*

class SlotArrangementLoader internal constructor() : DataLoader() {
	private val arrangements: MutableMap<String, List<List<String>>> = HashMap()

	fun getArrangement(slotName: String): List<List<String>>? {
		return arrangements[slotName]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/abstract/slot_arrangements.sdb")).use { set ->
			while (set.next()) {
				val arrangement: MutableList<List<String>> = ArrayList()
				for (slots in set.getText("slots").split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
					arrangement.add(java.util.List.of(*slots.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
				}
				arrangements[set.getText("iff")] = Collections.unmodifiableList(arrangement)
			}
		}
	}
}
