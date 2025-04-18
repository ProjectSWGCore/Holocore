/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.StandardCharsets

class StaticItemLoaderTest {
	
	companion object {
		@JvmStatic
		private fun staticItemInfos(): Collection<Arguments> {
			return ServerData.staticItems.items.map { Arguments.of(Named.of(it.itemName, it)) }
		}
	}
	
	private val encoder = StandardCharsets.US_ASCII.newEncoder()

	@ParameterizedTest
	@MethodSource("staticItemInfos")
	fun `object name is valid ASCII`(staticItemInfo: StaticItemLoader.StaticItemInfo) {
		assertTrue(encoder.canEncode(staticItemInfo.stringName)) { generateInvalidAsciiErrorMessage(staticItemInfo) }
	}
	
	private fun generateInvalidAsciiErrorMessage(staticItemInfo: StaticItemLoader.StaticItemInfo): String {
		val stringName = staticItemInfo.stringName

		// Detect invalid ASCII characters and print them as an array.
		val invalidChars = stringName.toCharArray().filter { !encoder.canEncode(it) }

		return "$stringName contains non-ASCII characters. These will not display correctly in the client. Please remove/replace these: $invalidChars"
	}
}