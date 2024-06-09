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
package com.projectswg.holocore.resources.support.data.server_info.loader.npc

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.data.server_info.loader.*
import java.io.File
import java.io.IOException
import java.util.function.Function
import java.util.stream.Collectors

class NpcStatLoader : DataLoader() {
	private val npcStatMap: MutableMap<Int, NpcStatInfo> = HashMap()

	operator fun get(level: Int): NpcStatInfo? {
		return npcStatMap[level]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/npc/npc_stats.sdb")).use { set ->
			npcStatMap.putAll(set.stream { NpcStatInfo(it) }.collect(Collectors.toMap({ obj: NpcStatInfo -> obj.level }, Function.identity())))
		}
	}

	class NpcStatInfo(set: SdbResultSet) {
		val level: Int = set.getInt("Level").toInt()
		val healthRegen: Int = set.getInt("HealthRegen").toInt()
		val actionRegen: Int = set.getInt("ActionRegen").toInt()
		val mindRegen: Int = set.getInt("MindRegen").toInt()
		val normalDetailStat: DetailNpcStatInfo = DetailNpcStatInfo(set, null)
		val eliteDetailStat: DetailNpcStatInfo = DetailNpcStatInfo(set, "Elite")
		val bossDetailStat: DetailNpcStatInfo = DetailNpcStatInfo(set, "Boss")
	}

	class DetailNpcStatInfo(set: SdbResultSet, prefix: String?) {
		val health: Int = get(set, prefix, "HP")
		val action: Int = get(set, prefix, "Action")
		val regen: Int = get(set, prefix, "Regen")
		val combatRegen: Int = get(set, prefix, "CombatRegen")
		val damagePerSecond: Int = get(set, prefix, "damagePerSecond")
		val toHit: Int = get(set, prefix, "ToHit")
		val def: Int = get(set, prefix, "Def")
		val armor: Int = get(set, prefix, "Armor")
		val xp: Int = get(set, prefix, "XP")

		companion object {
			private fun get(set: SdbResultSet, prefix: String?, name: String): Int {
				if (prefix == null) return set.getInt(name).toInt()
				return set.getInt(prefix + '_' + name).toInt()
			}
		}
	}
}
