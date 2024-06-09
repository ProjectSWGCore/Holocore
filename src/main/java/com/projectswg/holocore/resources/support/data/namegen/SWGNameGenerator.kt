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

import com.projectswg.common.data.encodables.tangible.Race
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.nio.charset.StandardCharsets
import java.util.concurrent.ThreadLocalRandom

class SWGNameGenerator {
	private val rules: MutableMap<String, Reference<RaceNameRule?>?> = HashMap()
	private val nameFilter = NameFilter()

	fun generateRaceName(race: Race): String {
		return generateName("race_" + race.species)
	}

	fun generateResourceName(): String {
		return generateName("resources")
	}

	private fun generateName(file: String): String {
		if (!nameFilter.isLoaded()) nameFilter.load()
		val ruleRef = rules[file]
		var rule = if ((ruleRef == null)) null else ruleRef.get()
		if (rule == null) rule = loadNamingRule(file)
		rules.replace(file, ruleRef, SoftReference(rule))

		return generateFilteredName(rule, true)
	}

	/**
	 * Generates a random name for the defined rule.
	 *
	 * @param rule    The rule to generate from
	 * @param surname Determines if a surname should be generated or not.
	 * @return Generated name in the form of a [String], as well as the surname dependent on chance if true
	 */
	private fun generateFilteredName(rule: RaceNameRule, surname: Boolean): String {
		val name = StringBuilder()
		do {
			name.setLength(0)
			do {
				name.append(generateName(rule))
			} while (name.length <= 0) // Some reason a name can be empty, I think it has to do with the removeExcessDuplications check.

			if (surname && shouldGenerateSurname(rule)) name.append(' ').append(generateFilteredName(rule, false))

			name.setCharAt(0, name[0].uppercaseChar())
		} while (!nameFilter.isValid(name.toString()))

		return name.toString()
	}

	private fun generateName(rule: RaceNameRule): String {
		val buffer = StringBuilder()
		var instructions = getRandomInstruction(rule)
		val l = instructions!!.length

		for (i in 0 until l) {
			val x = instructions!![0]
			val append = when (x) {
				'v'  -> removeExcessDuplications(rule.vowels, buffer.toString(), getRandomElementFrom(rule.vowels))!!
				'c'  -> removeExcessDuplications(rule.startConsonants, buffer.toString(), getRandomElementFrom(rule.startConsonants))!!
				'd'  -> removeExcessDuplications(rule.endConsonants, buffer.toString(), getRandomElementFrom(rule.endConsonants))!!
				'/'  -> "'"
				else -> ""
			}

			if (buffer.length + append.length >= rule.maxLength) break
			buffer.append(append)

			instructions = instructions.substring(1)
		}
		if (buffer.length == 0) return generateName(rule)
		return buffer.toString()
	}

	private fun getRandomInstruction(rule: RaceNameRule): String? {
		return getRandomElementFrom(rule.instructions)
	}

	private fun loadNamingRule(file: String): RaceNameRule {
		try {
			javaClass.getResourceAsStream("/namegen/$file.txt").use { stream ->
				if (stream == null) throw FileNotFoundException("/namegen/$file.txt")
				val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
				val rule = RaceNameRule()

				populateRule(rule, reader)
				return rule
			}
		} catch (e: IOException) {
			throw RuntimeException(e)
		}
	}

	@Throws(IOException::class)
	private fun populateRule(rule: RaceNameRule, reader: BufferedReader) {
		var populating = "NONE"
		var line: String
		while ((reader.readLine().also { line = it }) != null) {
			if (line.isEmpty()) continue

			if (line.startsWith("[")) {
				populating = getPopulateType(line)

				if (populating == "End") return
			} else if (!line.startsWith("#")) {
				when (populating) {
					"Settings"        -> populateSettings(rule, line)
					"Vowels"          -> rule.addVowel(line)
					"StartConsonants" -> rule.addStartConsonant(line)
					"EndConsonants"   -> rule.addEndConsant(line)
					"Instructions"    -> rule.addInstruction(line)
				}
			}
		}
		reader.close()
	}

	private fun getPopulateType(line: String): String {
		return when (line) {
			"[Settings]"        -> "Settings"
			"[Vowels]"          -> "Vowels"
			"[StartConsonants]" -> "StartConsonants"
			"[EndConsonants]"   -> "EndConsonants"
			"[Instructions]"    -> "Instructions"
			else                -> "End"
		}
	}

	private fun populateSettings(rule: RaceNameRule, line: String) {
		val key = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
		val value = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

		if ("SurnameChance" == key) {
			rule.surnameChance = value.toInt()
		}
		if ("MaxLength" == key) {
			rule.maxLength = value.toInt()
		}
	}

	companion object {
		private fun removeExcessDuplications(list: List<String?>?, orig: String, n: String?): String? {
			// Only checks the first and last for repeating
			if (orig.length <= 1) return n

			if (orig[orig.length - 1] == n!![0]) {
				if (!list!!.contains(orig[orig.length - 1].toString() + n[0])) {
					return removeExcessDuplications(list, orig, getRandomElementFrom(list))
				}
			}
			return n
		}

		private fun getRandomElementFrom(list: List<String?>?): String? {
			return list!![ThreadLocalRandom.current().nextInt(0, list.size - 1)]
		}

		private fun shouldGenerateSurname(rule: RaceNameRule): Boolean {
			return rule.surnameChance != 0 && (ThreadLocalRandom.current().nextInt(0, 100) <= rule.surnameChance)
		}
	}
}
