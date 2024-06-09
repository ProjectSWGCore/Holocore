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
package com.projectswg.holocore.resources.support.data.namegen

import me.joshlarson.jlcommon.log.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

class NameFilter {
	private val profaneWords: MutableList<String> = ArrayList()
	private val reservedWords: MutableList<String> = ArrayList()
	private val fictionNames: MutableList<String> = ArrayList()
	private val profaneStream: InputStream = Objects.requireNonNull(javaClass.getResourceAsStream("/namegen/filter/bad_word_list.txt"), "profaneStream")
	private val reservedStream: InputStream = Objects.requireNonNull(javaClass.getResourceAsStream("/namegen/filter/reserved_words.txt"), "reservedStream")
	private val fictionStream: InputStream = Objects.requireNonNull(javaClass.getResourceAsStream("/namegen/filter/fiction_reserved.txt"), "fictionStream")
	private val loaded = AtomicBoolean(false)

	fun isLoaded(): Boolean {
		return loaded.get()
	}

	fun load(): Boolean {
		if (loaded.getAndSet(true)) return true
		var success = load(profaneWords, profaneStream)
		success = load(reservedWords, reservedStream) && success
		success = load(fictionNames, fictionStream) && success
		return success
	}

	private fun load(list: MutableList<String>, input: InputStream): Boolean {
		try {
			BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
				list.clear()
				reader.lines().forEach { line ->
					val lowerline = line.lowercase()
					if (lowerline.isNotEmpty() && !list.contains(lowerline))
						list.add(lowerline)
				}
			}
			return true
		} catch (e: IOException) {
			Log.e(e)
			return false
		}
	}

	fun isValid(name: String): Boolean {
		val modified = cleanName(name)
		if (isEmpty(modified)) // Empty name
			return false
		if (containsBadCharacters(modified)) // Has non-alphabetic characters
			return false
		if (isProfanity(modified)) // Contains profanity
			return false
		if (isFictionallyInappropriate(modified)) return false
		if (isReserved(modified)) return false

		return modified == name // If we needed to remove double spaces, trim the ends, etc
	}

	fun cleanName(name: String): String {
		return WHITESPACE.matcher(name).replaceAll(" ").trim { it <= ' ' }
	}

	fun passesFilter(name: String): Boolean {
		return !isEmpty(name) && !containsBadCharacters(name) && !isProfanity(name)
	}

	fun isReserved(name: String): Boolean {
		return contains(reservedWords, name)
	}

	fun isFictionallyReserved(name: String): Boolean {
		return contains(fictionNames, name)
	}

	fun isFictionallyInappropriate(name: String): Boolean {
		var space = true
		for (element in name) {
			space = if (!Character.isAlphabetic(element.code)) true
			else if (Character.isUpperCase(element) && !space) return true
			else false
		}
		return false
	}

	fun isEmpty(name: String): Boolean {
		return name.length < 3
	}

	fun isProfanity(name: String): Boolean {
		return contains(profaneWords, name)
	}

	fun containsBadCharacters(word: String): Boolean {
		val max = IntArray(ALLOWED.size)
		var matched = false
		for (element in word) {
			if (!Character.isAlphabetic(element.code)) {
				for (a in ALLOWED.indices) {
					if (ALLOWED[a] == element) {
						if (++max[a] > MAX_ALLOWED[a]) // Increments and checks
							return true // More than what can be had

						if (matched && element != ' ') return true // Two unallowed characters in a row

						matched = true
						break
					}
				}
				if (!matched) // Some other un-allowed character
					return true
			} else matched = false
		}
		return false
	}

	private fun contains(list: List<String>, name: String): Boolean {
		val lowername = name.lowercase()
		for (str in list) if (lowername.length >= str.length && (lowername.startsWith(str) || lowername.endsWith(str))) return true
		val filtered = lowername.replace("[^a-zA-Z]".toRegex(), "")
		if (filtered != lowername) for (w in lowername.split("[^a-zA-Z]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) if (w.isNotEmpty() && contains(list, w)) return true
		for (str in list) if (filtered.length >= str.length && (filtered.startsWith(str) || filtered.endsWith(str))) return true
		return false
	}

	companion object {
		private val WHITESPACE: Pattern = Pattern.compile(" +")
		private val ALLOWED = charArrayOf(' ', '-', '\'')
		private val MAX_ALLOWED = intArrayOf(1, 1, 1)
	}
}
