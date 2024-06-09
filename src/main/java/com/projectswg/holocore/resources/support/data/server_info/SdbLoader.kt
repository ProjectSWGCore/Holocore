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

import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.*
import me.joshlarson.jlcommon.log.Log
import org.intellij.lang.annotations.Language
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.Spliterators.AbstractSpliterator
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Stream
import java.util.stream.StreamSupport

object SdbLoader {
	@Throws(IOException::class)
	fun load(file: File): SdbResultSet {
		val ext = getExtension(file)
		return when (ext) {
			"msdb" -> MasterSdbResultSet.load(file)
			"sdb"  -> SingleSdbResultSet.load(file)
			else   -> throw IllegalArgumentException("Invalid file! Expected either msdb or sdb")
		}
	}

	private fun getExtension(file: File): String {
		val ext = file.name.lowercase()
		val lastPeriod = ext.lastIndexOf('.')
		if (lastPeriod == -1) return ext
		return ext.substring(lastPeriod + 1)
	}

	interface SdbResultSet : Closeable, AutoCloseable {
		@Throws(IOException::class)
		override fun close()
		@Throws(IOException::class)
		fun next(): Boolean
		val columns: List<String>
		val file: File
		val line: Int

		/**
		 * Returns a sequential stream after applying the specified transformation to the SdbResultSet
		 * @param transform the transformation to apply
		 * @param <T> the type of the returned stream
		 * @return a new sequential stream to iterate over the SDB
		</T> */
		fun <T> stream(transform: Function<SdbResultSet, T>): Stream<T>

		/**
		 * Returns a parallel stream after applying the specified transformation to the SdbResultSet
		 * @param transform the transformation to apply
		 * @param <T> the type of the returned stream
		 * @return a new parallel stream to iterate over the SDB
		</T> */
		fun <T> parallelStream(transform: Function<SdbResultSet, T>): Stream<T>

		fun getTextArrayParser(@Language("RegExp") regex: String, defValue: String?): SdbTextColumnArraySet
		fun getIntegerArrayParser(@Language("RegExp") regex: String, defValue: Int): SdbIntegerColumnArraySet
		fun getLongArrayParser(@Language("RegExp") regex: String, defValue: Long): SdbLongColumnArraySet
		fun getRealArrayParser(@Language("RegExp") regex: String, defValue: Double): SdbRealColumnArraySet
		fun getBooleanArrayParser(@Language("RegExp") regex: String, defValue: Boolean): SdbBooleanColumnArraySet

		fun getText(index: Int): String
		fun getText(columnName: String): String

		fun getInt(index: Int): Long
		fun getInt(columnName: String): Long

		fun getReal(index: Int): Double
		fun getReal(columnName: String): Double

		fun getBoolean(index: Int): Boolean
		fun getBoolean(columnName: String): Boolean
	}

	private class MasterSdbResultSet private constructor(private val sdbList: List<SdbResultSet>) : SdbResultSet {
		private val sdbs: Iterator<SdbResultSet> = sdbList.iterator()
		private val sdb: AtomicReference<SdbResultSet?> = AtomicReference(if (sdbs.hasNext()) sdbs.next() else null)

		@Throws(IOException::class)
		override fun close() {
			resultSet?.close()
		}

		override fun <T> stream(transform: Function<SdbResultSet, T>): Stream<T> {
			return stream(transform, false)
		}

		override fun <T> parallelStream(transform: Function<SdbResultSet, T>): Stream<T> {
			return stream(transform, true)
		}

		fun <T> stream(transform: Function<SdbResultSet, T>, parallel: Boolean): Stream<T> {
			return StreamSupport.stream(Spliterators.spliterator(sdbList, Spliterator.NONNULL or Spliterator.IMMUTABLE or Spliterator.DISTINCT), parallel).flatMap { sdb: SdbResultSet -> sdb.stream(transform) }
		}

		@Throws(IOException::class)
		override fun next(): Boolean {
			var set = resultSet ?: return false
			while (!set.next()) {
				set.close()
				if (!sdbs.hasNext()) {
					sdb.set(null)
					return false // bummer
				}
				set = sdbs.next()
				sdb.set(set)
			}
			return true
		}

		override val columns: List<String>
			get() = resultSet!!.columns

		override val file: File
			get() = resultSet!!.file

		override val line: Int
			get() = resultSet!!.line

		override fun getTextArrayParser(@Language("RegExp") regex: String, defValue: String?): SdbTextColumnArraySet {
			return SdbTextColumnArraySet(regex, defValue)
		}

		override fun getIntegerArrayParser(@Language("RegExp") regex: String, defValue: Int): SdbIntegerColumnArraySet {
			return SdbIntegerColumnArraySet(regex, defValue)
		}

		override fun getLongArrayParser(@Language("RegExp") regex: String, defValue: Long): SdbLongColumnArraySet {
			return SdbLongColumnArraySet(regex, defValue)
		}

		override fun getRealArrayParser(@Language("RegExp") regex: String, defValue: Double): SdbRealColumnArraySet {
			return SdbRealColumnArraySet(regex, defValue)
		}

		override fun getBooleanArrayParser(@Language("RegExp") regex: String, defValue: Boolean): SdbBooleanColumnArraySet {
			return SdbBooleanColumnArraySet(regex, defValue)
		}

		override fun getText(index: Int): String {
			return resultSet!!.getText(index)
		}

		override fun getText(columnName: String): String {
			return resultSet!!.getText(columnName)
		}

		override fun getInt(index: Int): Long {
			return resultSet!!.getInt(index)
		}

		override fun getInt(columnName: String): Long {
			return resultSet!!.getInt(columnName)
		}

		override fun getReal(index: Int): Double {
			return resultSet!!.getReal(index)
		}

		override fun getReal(columnName: String): Double {
			return resultSet!!.getReal(columnName)
		}

		override fun getBoolean(index: Int): Boolean {
			return resultSet!!.getBoolean(index)
		}

		override fun getBoolean(columnName: String): Boolean {
			return resultSet!!.getBoolean(columnName)
		}

		private val resultSet: SdbResultSet?
			get() = sdb.get()

		companion object {
			@Throws(IOException::class)
			fun load(file: File): MasterSdbResultSet {
				val sets: MutableList<SdbResultSet> = ArrayList()
				val parentFile = file.parentFile
				SingleSdbResultSet.load(file).use { msdb ->
					while (msdb.next()) {
						if (msdb.getBoolean(1)) // is enabled
							sets.add(SdbLoader.load(File(parentFile, msdb.getText(0)))) // relative file path
					}
				}
				return MasterSdbResultSet(sets)
			}
		}
	}

	private class SingleSdbResultSet private constructor(override val file: File) : SdbResultSet {
		private val columnIndices: MutableMap<String, Int> = HashMap()
		private val lineNumber = AtomicLong(0)
		private var columnNames: Array<String>?
		private var columnValues: Array<String>?
		private var input: BufferedReader?

		init {
			this.columnNames = null
			this.columnValues = null
			this.input = null
		}

		@Throws(IOException::class)
		override fun close() {
			input!!.close()
		}

		override fun <T> stream(transform: Function<SdbResultSet, T>): Stream<T> {
			return stream(transform, false)
		}

		override fun <T> parallelStream(transform: Function<SdbResultSet, T>): Stream<T> {
			return stream(transform, true)
		}

		fun <T> stream(transform: Function<SdbResultSet, T>, parallel: Boolean): Stream<T> {
			val resultSet = ThreadLocal.withInitial { ParallelSdbResultSet(file, columnIndices, columnNames) }
			return StreamSupport.stream(SdbSpliterator(file, input, lineNumber), parallel).map { e: Map.Entry<Long, String> ->
				val set = resultSet.get()
				set.load(e.value, e.key)
				transform.apply(set)
			}
		}

		override fun next(): Boolean {
			var line: String?
			do {
				lineNumber.incrementAndGet()
				try {
					line = input!!.readLine()
					if (line == null) return false
				} catch (e: IOException) {
					return false
				}
			} while (line!!.isEmpty())
			var index = 0
			val columnCount = columnValues!!.size
			for (column in 0 until columnCount) {
				var nextIndex = line.indexOf('\t', index)
				if (nextIndex == -1) {
					if (column + 1 < columnCount) {
						Log.e("Invalid entry in sdb: %s on line %d - invalid number of columns!", file, lineNumber.get(), column + 1)
						return false
					}
					nextIndex = line.length
				}
				columnValues!![column] = line.substring(index, nextIndex)
				index = nextIndex + 1
			}
			return true
		}

		override val columns: List<String>
			get() = columnNames?.toList() ?: listOf()

		override val line: Int
			get() = lineNumber.get().toInt()

		override fun getTextArrayParser(@Language("RegExp") regex: String, defValue: String?): SdbTextColumnArraySet {
			return SdbTextColumnArraySet(regex, defValue)
		}

		override fun getIntegerArrayParser(@Language("RegExp") regex: String, defValue: Int): SdbIntegerColumnArraySet {
			return SdbIntegerColumnArraySet(regex, defValue)
		}

		override fun getLongArrayParser(@Language("RegExp") regex: String, defValue: Long): SdbLongColumnArraySet {
			return SdbLongColumnArraySet(regex, defValue)
		}

		override fun getRealArrayParser(@Language("RegExp") regex: String, defValue: Double): SdbRealColumnArraySet {
			return SdbRealColumnArraySet(regex, defValue)
		}

		override fun getBooleanArrayParser(@Language("RegExp") regex: String, defValue: Boolean): SdbBooleanColumnArraySet {
			return SdbBooleanColumnArraySet(regex, defValue)
		}

		override fun getText(index: Int): String {
			return columnValues!![index]
		}

		override fun getText(columnName: String): String {
			assert(columnIndices.containsKey(columnName)) { "column $columnName does not exist in sdb $file" }
			return columnValues!![columnIndices[columnName]!!]
		}

		override fun getInt(index: Int): Long {
			try {
				return columnValues!![index].toLong()
			} catch (e: NumberFormatException) {
				throw NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index + 1))
			}
		}

		override fun getInt(columnName: String): Long {
			assert(columnIndices.containsKey(columnName)) { "column $columnName does not exist in sdb $file" }
			return getInt(columnIndices[columnName]!!)
		}

		override fun getReal(index: Int): Double {
			try {
				return columnValues!![index].toDouble()
			} catch (e: NumberFormatException) {
				throw NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index + 1))
			}
		}

		override fun getReal(columnName: String): Double {
			assert(columnIndices.containsKey(columnName)) { "column $columnName does not exist in sdb $file" }
			return getReal(columnIndices[columnName]!!)
		}

		override fun getBoolean(index: Int): Boolean {
			return columnValues!![index].equals("true", ignoreCase = true)
		}

		override fun getBoolean(columnName: String): Boolean {
			assert(columnIndices.containsKey(columnName)) { "column $columnName does not exist in sdb $file" }
			return getBoolean(columnIndices[columnName]!!)
		}

		@Throws(IOException::class)
		private fun load() {
			input = BufferedReader(FileReader(file, StandardCharsets.UTF_8), 128 * 1024)
			loadHeader(input!!.readLine())
			input!!.readLine()
		}

		private fun loadHeader(columnsStr: String?) {
			if (columnsStr == null) {
				Log.e("Invalid SDB header: %s - nonexistent", file)
				return
			}
			columnNames = columnsStr.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			columnValues = Array(columnNames!!.size) { "" }
			for (i in columnNames!!.indices) {
				columnIndices[columnNames!![i]] = i
			}
			lineNumber.set(2)
		}

		companion object {
			@Throws(IOException::class)
			fun load(file: File): SingleSdbResultSet {
				val sdb = SingleSdbResultSet(file)
				sdb.load()
				return sdb
			}
		}
	}

	private class ParallelSdbResultSet(override val file: File, private val columnIndices: Map<String, Int>, private val columnNames: Array<String>?) : SdbResultSet {
		private val lineNumber = AtomicLong(0)
		private val columnValues: Array<String> = Array(columnIndices.size) { "" }

		override fun close() {
			throw UnsupportedOperationException("Cannot close a parallel sdb")
		}

		override fun next(): Boolean {
			throw UnsupportedOperationException("Cannot iterate a parallel sdb")
		}

		override fun <T> stream(transform: Function<SdbResultSet, T>): Stream<T> {
			throw UnsupportedOperationException("Cannot iterate a parallel sdb")
		}

		override fun <T> parallelStream(transform: Function<SdbResultSet, T>): Stream<T> {
			throw UnsupportedOperationException("Cannot iterate a parallel sdb")
		}

		fun load(line: String, lineNumber: Long) {
			var index = 0
			val columnCount = columnValues.size
			for (column in 0 until columnCount) {
				var nextIndex = line.indexOf('\t', index)
				if (nextIndex == -1) {
					if (column + 1 < columnCount) {
						Log.e("Invalid entry in sdb: %s on line %d - invalid number of columns!", file, lineNumber, column + 1)
						return
					}
					nextIndex = line.length
				}
				columnValues[column] = line.substring(index, nextIndex)
				index = nextIndex + 1
			}
		}

		override val columns: List<String>
			get() = columnNames?.toList() ?: listOf()

		override val line: Int
			get() = lineNumber.get().toInt()

		override fun getTextArrayParser(@Language("RegExp") regex: String, defValue: String?): SdbTextColumnArraySet {
			return SdbTextColumnArraySet(regex, defValue)
		}

		override fun getIntegerArrayParser(@Language("RegExp") regex: String, defValue: Int): SdbIntegerColumnArraySet {
			return SdbIntegerColumnArraySet(regex, defValue)
		}

		override fun getLongArrayParser(@Language("RegExp") regex: String, defValue: Long): SdbLongColumnArraySet {
			return SdbLongColumnArraySet(regex, defValue)
		}

		override fun getRealArrayParser(@Language("RegExp") regex: String, defValue: Double): SdbRealColumnArraySet {
			return SdbRealColumnArraySet(regex, defValue)
		}

		override fun getBooleanArrayParser(@Language("RegExp") regex: String, defValue: Boolean): SdbBooleanColumnArraySet {
			return SdbBooleanColumnArraySet(regex, defValue)
		}

		override fun getText(index: Int): String {
			return columnValues[index]
		}

		override fun getText(columnName: String): String {
			assert(columnIndices.containsKey(columnName)) { "column $columnName does not exist" }
			return columnValues[columnIndices[columnName]!!]
		}

		override fun getInt(index: Int): Long {
			try {
				return columnValues[index].toLong()
			} catch (e: NumberFormatException) {
				throw NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index + 1))
			}
		}

		override fun getInt(columnName: String): Long {
			assert(columnIndices.containsKey(columnName)) { "column $columnName does not exist" }
			return getInt(columnIndices[columnName]!!)
		}

		override fun getReal(index: Int): Double {
			try {
				return columnValues[index].toDouble()
			} catch (e: NumberFormatException) {
				throw NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index + 1))
			}
		}

		override fun getReal(columnName: String): Double {
			assert(columnIndices.containsKey(columnName)) { "column $columnName does not exist" }
			return getReal(columnIndices[columnName]!!)
		}

		override fun getBoolean(index: Int): Boolean {
			return columnValues[index].equals("true", ignoreCase = true)
		}

		override fun getBoolean(columnName: String): Boolean {
			assert(columnIndices.containsKey(columnName)) { "column $columnName does not exist" }
			return getBoolean(columnIndices[columnName]!!)
		}
	}

	private class SdbSpliterator(private val file: File, private val input: BufferedReader?, private val lineNumber: AtomicLong) : AbstractSpliterator<Map.Entry<Long, String>>(Long.MAX_VALUE, ORDERED or NONNULL or IMMUTABLE or DISTINCT) {
		@Synchronized
		override fun tryAdvance(action: Consumer<in Map.Entry<Long, String>>): Boolean {
			var line: String
			var lineNumber: Long = -1
			try {
				synchronized(input!!) {
					lineNumber = this.lineNumber.incrementAndGet()
					line = input.readLine() ?: return false
				}
				if (line.isEmpty()) return true
				action.accept(java.util.Map.entry(lineNumber, line))
				return true
			} catch (e: IOException) {
				throw RuntimeException(e)
			} catch (t: Throwable) {
				if (t.cause == null) {
					Log.e("Failed to load line %d on SDB %s due to %s: %s", lineNumber, file, t.javaClass.name, t.message)
				} else {
					Log.e("Failed to load line %d on SDB %s", lineNumber, file)
					var temp: Throwable? = t
					while (temp != null) {
						Log.e("    %s: %s", temp.javaClass.name, temp.message)
						temp = temp.cause!!
					}
				}
				return true
			}
		}
	}
}
