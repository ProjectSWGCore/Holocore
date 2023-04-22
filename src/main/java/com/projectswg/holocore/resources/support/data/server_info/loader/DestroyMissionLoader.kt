/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
import java.io.File

class DestroyMissionLoader : DataLoader() {

	private val terrainToGeneralDestroyMissions = mutableMapOf<Terrain, MutableCollection<DestroyMissionInfo>>()
	private val terrainToRebelDestroyMissions = mutableMapOf<Terrain, MutableCollection<DestroyMissionInfo>>()
	private val terrainToImperialDestroyMissions = mutableMapOf<Terrain, MutableCollection<DestroyMissionInfo>>()

	fun getGeneralDestroyMissions(terrain: Terrain): Collection<DestroyMissionInfo> {
		return terrainToGeneralDestroyMissions.getOrElse(terrain) {
			emptySet()
		}
	}
	
	fun getRebelDestroyMissions(terrain: Terrain): Collection<DestroyMissionInfo> {
		return terrainToRebelDestroyMissions.getOrElse(terrain) {
			emptySet()
		}
	}
	
	fun getImperialDestroyMissions(terrain: Terrain): Collection<DestroyMissionInfo> {
		return terrainToImperialDestroyMissions.getOrElse(terrain) {
			emptySet()
		}
	}

	override fun load() {
		val set = SdbLoader.load(File("serverdata/missions/destroy/main.msdb"))

		set.use {
			while (set.next()) {
				val terrain = Terrain.valueOf(set.getText("terrain"))
				val destroyMissionInfo = DestroyMissionInfo(set)
				val destroyMissionMap = when (set.getText("destroy_type")) {
					"general"  -> terrainToGeneralDestroyMissions
					"imperial" -> terrainToImperialDestroyMissions
					"rebel"    -> terrainToRebelDestroyMissions
					else       -> throw IllegalArgumentException("Unknown destroy type ${set.getText("destroy_type")}")
				}

				if (destroyMissionMap.contains(terrain)) {
					destroyMissionMap[terrain]?.add(destroyMissionInfo)
				} else {
					destroyMissionMap[terrain] = mutableSetOf(destroyMissionInfo)
				}
			}
		}
	}

	class DestroyMissionInfo(set: SdbResultSet) {
		val stringFile: String = set.getText("string_file")
		val titleKey: String = set.getText("title_key")
		val descriptionKey: String = set.getText("description_key")
		val creator: String = set.getText("creator")
		val target: String = set.getText("target")
		val dynamicId: String = set.getText("dynamic_id")
	}
}