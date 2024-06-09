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
package com.projectswg.holocore.resources.support.data.server_info

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import org.intellij.lang.annotations.Language
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

abstract class SdbColumnArraySet private constructor(@Language("RegExp") regex: String) {
	private val pattern: Pattern = Pattern.compile(regex)
	private val mappedInfo: MutableMap<File, MappedInfo> = ConcurrentHashMap()

	fun size(set: SdbResultSet): Int {
		return mappedInfo.computeIfAbsent(set.file) { MappedInfo(set) }.size
	}

	protected fun getMappedEntries(set: SdbResultSet): Collection<Map.Entry<Int, Int>> {
		return mappedInfo.computeIfAbsent(set.file) { MappedInfo(set) }.mappedColumns.entries
	}

	class SdbTextColumnArraySet internal constructor(@Language("RegExp") regex: String, private val defValue: String?) : SdbColumnArraySet(regex) {
		private val cachedArray = ThreadLocal<Array<String>>()

		fun getArray(set: SdbResultSet?): Array<String> {
			var cachedArray = cachedArray.get()
			val size = size(set!!)
			if (cachedArray == null || cachedArray.size != size) {
				cachedArray = Array(size) { "" }
				this.cachedArray.set(cachedArray)
			}

			Arrays.fill(cachedArray, "")
			for ((key, value) in getMappedEntries(set)) {
				cachedArray[key] = set.getText(value)
			}
			return cachedArray
		}
	}

	class SdbIntegerColumnArraySet internal constructor(@Language("RegExp") regex: String, private val defValue: Int) : SdbColumnArraySet(regex) {
		private val cachedArray = ThreadLocal<IntArray>()

		fun getArray(set: SdbResultSet?): IntArray {
			var cachedArray = cachedArray.get()
			val size = size(set!!)
			if (cachedArray == null || cachedArray.size != size) {
				cachedArray = IntArray(size)
				this.cachedArray.set(cachedArray)
			}

			Arrays.fill(cachedArray, defValue)
			for ((key, value) in getMappedEntries(set)) {
				cachedArray[key] = set.getInt(value).toInt()
			}
			return cachedArray
		}
	}

	class SdbLongColumnArraySet internal constructor(@Language("RegExp") regex: String, private val defValue: Long) : SdbColumnArraySet(regex) {
		private val cachedArray = ThreadLocal<LongArray>()

		fun getArray(set: SdbResultSet?): LongArray {
			var cachedArray = cachedArray.get()
			val size = size(set!!)
			if (cachedArray == null || cachedArray.size != size) {
				cachedArray = LongArray(size)
				this.cachedArray.set(cachedArray)
			}

			Arrays.fill(cachedArray, defValue)
			for ((key, value) in getMappedEntries(set)) {
				cachedArray[key] = set.getInt(value)
			}
			return cachedArray
		}
	}

	class SdbRealColumnArraySet internal constructor(@Language("RegExp") regex: String, private val defValue: Double) : SdbColumnArraySet(regex) {
		private val cachedArray = ThreadLocal<DoubleArray>()

		fun getArray(set: SdbResultSet?): DoubleArray {
			var cachedArray = cachedArray.get()
			val size = size(set!!)
			if (cachedArray == null || cachedArray.size != size) {
				cachedArray = DoubleArray(size)
				this.cachedArray.set(cachedArray)
			}

			Arrays.fill(cachedArray, defValue)
			for ((key, value) in getMappedEntries(set)) {
				cachedArray[key] = set.getReal(value)
			}
			return cachedArray
		}
	}

	class SdbBooleanColumnArraySet internal constructor(@Language("RegExp") regex: String, private val defValue: Boolean) : SdbColumnArraySet(regex) {
		private val cachedArray = ThreadLocal<BooleanArray>()

		fun getArray(set: SdbResultSet?): BooleanArray {
			var cachedArray = cachedArray.get()
			val size = size(set!!)
			if (cachedArray == null || cachedArray.size != size) {
				cachedArray = BooleanArray(size)
				this.cachedArray.set(cachedArray)
			}

			Arrays.fill(cachedArray, defValue)
			for ((key, value) in getMappedEntries(set)) {
				cachedArray[key] = set.getBoolean(value)
			}
			return cachedArray
		}
	}

	private inner class MappedInfo(set: SdbResultSet) {
		val mappedColumns: Map<Int, Int>
		val size: Int

		init {
			val mappedColumns: MutableMap<Int, Int> = HashMap()
			var size = 0
			for ((columnIndex, column) in set.columns.withIndex()) {
				val matcher = pattern.matcher(column)
				val match = matcher.matches()
				if (match && matcher.groupCount() == 1) {
					val arrayIndexStr = matcher.group(1)
					try {
						val arrayIndex = Integer.parseUnsignedInt(arrayIndexStr)
						if (arrayIndex >= size) size = arrayIndex + 1
						mappedColumns[arrayIndex] = columnIndex
					} catch (e: NumberFormatException) {
						throw IllegalArgumentException("invalid pattern. The first capturing group must be only digits")
					}
				} else require(!match) { "invalid pattern. Regex must have capturing group for array index" }
			}

			this.mappedColumns = Collections.unmodifiableMap(mappedColumns)
			this.size = size
		}
	}
}
