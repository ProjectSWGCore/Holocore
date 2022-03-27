/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.data.server_info.loader.combat

import com.projectswg.common.data.encodables.tangible.PvpFaction
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

class FactionLoader : DataLoader() {
	
	private var factions: Map<String, Faction> = HashMap()
	
	fun getFaction(faction: String): Faction? {
		return factions[faction]
	}
	
	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/faction/faction_datatable.sdb")).use { set ->
			factions = set.stream { Faction(it) }.collect(Collectors.toMap({ it.name }, { it }))
		}
	}
	
	class Faction(set: SdbResultSet) {
		
		val name = set.getText("factionName").toLowerCase(Locale.US)
		val isPlayerAllowed = set.getBoolean("playerAllowed")
		val isAggressive = set.getBoolean("isAggro")
		val isPvP = set.getBoolean("isPvP")
		val pvpFaction = processPvpFaction(set.getText("pvpFaction"))
		private val enemies = processList(set.getText("enemies"))
		private val allies = processList(set.getText("allies"))
		val combatFactor = set.getInt("combatFactor").toInt()
		
		fun isEnemy(faction: String) = enemies.contains(faction.toLowerCase(Locale.US))
		fun isEnemy(faction: Faction) = enemies.contains(faction.name)
		
		fun isAlly(faction: String) = name == faction.toLowerCase() || allies.contains(faction.toLowerCase(Locale.US))
		fun isAlly(faction: Faction) = name == faction.name || allies.contains(faction.name)
		
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			
			return name == (other as Faction).name
		}
		
		override fun hashCode(): Int {
			return name.hashCode()
		}
		
		override fun toString(): String {
			return "Faction(name=$name isPlayerAllowed=$isPlayerAllowed isAggressive=$isAggressive isPvP=$isPvP enemies=$enemies allies=$allies)"
		}
		
		private fun processPvpFaction(faction: String): PvpFaction {
			return when (faction.toLowerCase(Locale.US)) {
				"imperial" -> PvpFaction.IMPERIAL
				"rebel" -> PvpFaction.REBEL
				else -> PvpFaction.NEUTRAL
			}
		}
		
		private fun processList(listStr: String): List<String> {
			if (listStr == "-" || listStr.isBlank())
				return emptyList()
			return listStr.toLowerCase(Locale.US).split(';')
		}
		
	}
	
}
