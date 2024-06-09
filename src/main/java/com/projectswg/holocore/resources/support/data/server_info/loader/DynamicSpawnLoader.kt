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

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.SpawnerFlag
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.IOException
import java.util.*

class DynamicSpawnLoader internal constructor() : DataLoader() {
	private val terrainSpawns = EnumMap<Terrain, Collection<DynamicSpawnInfo>>(Terrain::class.java)
	private val dynamicIdToDynamicSpawn = HashMap<String, DynamicSpawnInfo>()

	/**
	 * Fetches dynamic spawn information for the given terrain.
	 * @param terrain to find spawn information for
	 * @return collection of spawn information. Never `null`, can be empty and is unmodifiable.
	 */
	fun getSpawnInfos(terrain: Terrain): Collection<DynamicSpawnInfo> {
		return terrainSpawns[terrain] ?: emptyList()
	}

	fun getSpawnInfo(dynamicId: String): DynamicSpawnInfo? {
		return dynamicIdToDynamicSpawn[dynamicId]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/spawn/dynamic/dynamic_spawns.sdb")).use { set ->
			val terrainSpawns = EnumMap<Terrain, ArrayList<DynamicSpawnInfo>>(Terrain::class.java)
			while (set.next()) {
				val planetsCellValue = set.getText("planets")
				val planets = planetsCellValue.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				val dynamicSpawnInfo = DynamicSpawnInfo(set)

				for (planet in planets) {
					val terrain = checkNotNull(Terrain.getTerrainFromName(planet)) { "unable to find terrain by name $planet" }
					val dynamicSpawnInfos = terrainSpawns.computeIfAbsent(terrain) { ArrayList() }
					dynamicSpawnInfos.add(dynamicSpawnInfo)
				}

				dynamicIdToDynamicSpawn[dynamicSpawnInfo.dynamicId] = dynamicSpawnInfo
			}
			for ((terrain, spawns) in terrainSpawns)
				this.terrainSpawns[terrain] = Collections.unmodifiableCollection(spawns)
		}
	}

	class DynamicSpawnInfo(set: SdbResultSet) {
		val dynamicId: String = set.getText("dynamic_id")
		val lairIds: Array<String> = set.getText("lair_id").split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val npcBoss: String = set.getText("npc_boss")
		val npcElite: String = set.getText("npc_elite")
		val npcNormal1: String = set.getText("npc_normal_1")
		val npcNormal2: String = set.getText("npc_normal_2")
		val npcNormal3: String = set.getText("npc_normal_3")
		var npcNormal4: String = set.getText("npc_normal_4")
		private val npcNormal5: String? = null
		private val npcNormal6: String? = null
		private val npcNormal7: String? = null
		private val npcNormal8: String? = null
		private val npcNormal9: String? = null
		val spawnerFlag: SpawnerFlag

		init {
			this.npcNormal4 = set.getText("npc_normal_5")
			this.npcNormal4 = set.getText("npc_normal_6")
			this.npcNormal4 = set.getText("npc_normal_7")
			this.npcNormal4 = set.getText("npc_normal_8")
			this.npcNormal4 = set.getText("npc_normal_9")
			this.spawnerFlag = readSpawnerFlag(dynamicId, set)
		}

		private fun readSpawnerFlag(id: String, set: SdbResultSet): SpawnerFlag {
			val columnName = "attackable"

			try {
				return SpawnerFlag.valueOf(set.getText(columnName))
			} catch (e: IllegalArgumentException) {
				Log.w("Unknown attackable flag for dynamic_id '%s': '%s'", id, set.getText(columnName))
				return SpawnerFlag.INVULNERABLE
			}
		}
	}
}
