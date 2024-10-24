/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.data.CRC
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbRealColumnArraySet
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbTextColumnArraySet
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException

class BuffLoader internal constructor() : DataLoader() {
	private val buffsByCrc: MutableMap<CRC, BuffInfo> = HashMap()

	fun getBuff(crc: CRC): BuffInfo? {
		return buffsByCrc[crc]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/buff/buff.sdb")).use { set ->
			val effectParams = set.getTextArrayParser("effect([0-9]+)_param", null)
			val effectValues = set.getRealArrayParser("effect([0-9]+)_value", 0.0)
			while (set.next()) {
				val buff = BuffInfo(set, effectParams, effectValues)
				buffsByCrc[buff.crc] = buff
			}
		}
	}

	class BuffInfo private constructor(val name: String, val group1: String, val group2: String, val block: String, val priority: Int, val icon: String, val duration: Double, private val effectNames: Array<String>, private val effectValues: DoubleArray, val state: String, val callback: String, val particle: String, val visible: Int, val isDebuff: Boolean) {
		val crc: CRC = CRC(name.lowercase())

		constructor(set: SdbResultSet, effectNames: SdbTextColumnArraySet, effectValues: SdbRealColumnArraySet) : this(
			set.getText("name"), set.getText("group1"), set.getText("group2"), set.getText("block"), set.getInt("priority").toInt(), set.getText("icon"), set.getReal("duration"), effectNames.getArray(set).clone(), effectValues.getArray(set).clone(), set.getText("state"), set.getText("callback"), set.getText("particle"), set.getInt("visible").toInt(), set.getBoolean("debuff")
		) {
			assert(this.effectNames.size == this.effectValues.size) { "effect params and effect values differ in size" }
		}

		fun getEffectNames(): Array<String> {
			return effectNames.clone()
		}

		fun getEffectName(index: Int): String {
			return effectNames[index]
		}

		fun getEffectValues(): DoubleArray {
			return effectValues.clone()
		}

		fun getEffectValue(index: Int): Double {
			return effectValues[index]
		}

		val effects: Int
			get() = effectNames.size
	}
}
