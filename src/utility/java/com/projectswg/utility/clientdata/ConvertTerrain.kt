/***********************************************************************************
 * Copyright (c) 2022 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.utility.clientdata

import com.projectswg.common.data.location.Terrain
import com.projectswg.common.data.swgiff.IffForm
import com.projectswg.common.data.swgiff.parsers.SWGParser
import com.projectswg.common.data.swgiff.parsers.terrain.TerrainTemplate
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile


internal class ConvertTerrain : Converter {
	
	override fun convert() {
		println("Converting terrain...")
		try {
			var beforeSize = 0L
			var afterSize = 0L
			for (terrain in TERRAIN_LIST) {
				val inputFile = File("clientdata", terrain.file)
				val terrainTemplate = SWGParser.parse<TerrainTemplate>(inputFile) ?: continue
				val output = IffForm.write(terrainTemplate.write())
				val outputFile = File("serverdata", terrain.file)
				if (!outputFile.exists())
					outputFile.createNewFile()
				RandomAccessFile(outputFile, "rw").channel.use {
					it.truncate(output.remaining().toLong())
					while (output.hasRemaining()) {
						it.write(output)
					}
				}
				SWGParser.parse<TerrainTemplate>(outputFile)
				println("%-25s [%2.2f%%] ${inputFile.length()} -> ${outputFile.length()}".format("$terrain:", 100 - outputFile.length().toFloat() / inputFile.length() * 100))
				beforeSize += inputFile.length()
				afterSize += outputFile.length()
				
				for (bitmap in terrainTemplate.bitmapGroup.bitmaps.values) {
					val bitmapFile = bitmap.file ?: continue
					if (terrainTemplate.isBitmapReferenced(bitmap.familyId))
						File("clientdata", bitmapFile).copyTo(File("serverdata", bitmapFile))
				}
			}
			println("$beforeSize -> $afterSize")
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}
	
	companion object {
		
		private val TERRAIN_LIST = listOf(
			Terrain.CORELLIA,
			Terrain.DANTOOINE,
			Terrain.DATHOMIR,
			Terrain.ENDOR,
			Terrain.KASHYYYK_DEAD_FOREST,
			Terrain.KASHYYYK_HUNTING,
			Terrain.KASHYYYK_MAIN,
			Terrain.KASHYYYK_NORTH_DUNGEONS,
			Terrain.KASHYYYK_POB_DUNGEONS,
			Terrain.KASHYYYK_RRYATT_TRAIL,
			Terrain.KASHYYYK_SOUTH_DUNGEONS,
			Terrain.KASHYYYK,
			Terrain.LOK,
			Terrain.MUSTAFAR,
			Terrain.NABOO,
			Terrain.RORI,
			Terrain.TAANAB,
			Terrain.TALUS,
			Terrain.TATOOINE,
			Terrain.UMBRA,
			Terrain.YAVIN4,
		)
		
	}
	
}