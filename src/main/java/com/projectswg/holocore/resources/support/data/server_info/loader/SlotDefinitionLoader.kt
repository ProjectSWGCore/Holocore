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
import kotlin.collections.HashMap

class SlotDefinitionLoader internal constructor() : DataLoader() {
	private val slotDefinitions: MutableMap<String, SlotDefinition> = HashMap()

	fun getSlotDefinition(slotName: String): SlotDefinition? = slotDefinitions[slotName]

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/abstract/slot_definitions.sdb")).use { set ->
			while (set.next()) {
				val def = SlotDefinition(set)
				slotDefinitions[def.name] = def
			}
		}
	}

	class SlotDefinition(set: SdbResultSet) {
		// slotName	global	modifiable	observeWithParent	exposeToWorld
		val name: String = set.getText("slotName")
		val isGlobal: Boolean = set.getBoolean("global")
		val isModifiable: Boolean = set.getBoolean("modifiable")
		val isObserveWithParent: Boolean = set.getBoolean("observeWithParent")
		val isExposeToWorld: Boolean = set.getBoolean("exposeToWorld")

		override fun hashCode(): Int {
			return name.hashCode()
		}

		override fun equals(other: Any?): Boolean {
			return other is SlotDefinition && other.name == name
		}
	}
}
