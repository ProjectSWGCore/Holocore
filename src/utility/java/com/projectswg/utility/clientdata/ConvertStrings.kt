/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.utility.clientdata

import com.projectswg.common.data.swgfile.ClientStringParser
import com.projectswg.holocore.utilities.SdbGenerator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

internal class ConvertStrings : Converter {

	override fun convert() {
		println("Converting clientdata strings into $OUTPUT_SDB_PATH")
		Files.walk(File(CLIENTDATA).toPath()).use { pathStream ->
			SdbGenerator(File(OUTPUT_SDB_PATH)).use { sdb ->
				sdb.writeColumnNames("file", "key", "value")
				pathStream.filter { it.fileName.toString().endsWith(".stf") }.sorted().forEach { convertPath(it, sdb) }
			}
		}
	}

	private fun convertPath(path: Path, sdb: SdbGenerator) {
		val fullPathString = path.invariantSeparatorsPathString
		val swgFileName = fullPathString.substring(fullPathString.indexOf(CLIENTDATA) + CLIENTDATA.length + 1, fullPathString.length - 4)
		val datatable = ClientStringParser.parse(path.toFile(), swgFileName)
		for ((key, value) in datatable.strings.entries.sortedBy { it.key.toString() }) {
			sdb.writeLine(key.file, key.key, value.replace("\n", "\\n").replace("\t", "\\t"))
		}
	}

	companion object {
		private const val CLIENTDATA = "clientdata/string/en"
		private const val OUTPUT_SDB_PATH = "serverdata/strings/strings.sdb"
	}
}
