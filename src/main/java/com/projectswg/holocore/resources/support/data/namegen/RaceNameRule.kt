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

import java.util.*
import kotlin.collections.ArrayList

internal class RaceNameRule {
	private val _vowels: MutableList<String> = ArrayList()
	val vowels: List<String>
		get() { return Collections.unmodifiableList(_vowels) }

	private val _startConsonants: MutableList<String> = ArrayList()
	val startConsonants: List<String>
		get() { return Collections.unmodifiableList(_startConsonants) }

	private val _endConsonants: MutableList<String> = ArrayList()
	val endConsonants: List<String>
		get() { return Collections.unmodifiableList(_endConsonants) }

	private val _instructions: MutableList<String> = ArrayList()
	val instructions: List<String>
		get() { return Collections.unmodifiableList(_instructions) }
	var surnameChance: Int = 0
	var maxLength: Int = 15

	fun addVowel(s: String) {
		_vowels.add(s)
	}

	fun addStartConsonant(s: String) {
		_startConsonants.add(s)
	}

	fun addEndConsant(s: String) {
		_endConsonants.add(s)
	}

	fun addInstruction(s: String) {
		_instructions.add(s)
	}
}
