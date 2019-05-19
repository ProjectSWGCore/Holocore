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

package com.projectswg.holocore.resources.support.data.server_info.loader.npc

import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.Collectors.groupingBy
import kotlin.math.min

class LootTableLoader : DataLoader() {
	
	private var lootTables: Map<String, Map<String, List<LootTableItem>>> = HashMap()
	
	fun getLootTableItem(table: String, difficulty: String, level: Int): LootTableItem? {
		return lootTables[table]?.get(difficulty)?.firstOrNull { it.minLevel <= level && it.maxLevel > level }
	}
	
	@Throws(IOException::class)
	public override fun load() {
		SdbLoader.load(File("serverdata/loot/loot_table.sdb")).use { set ->
			val itemGroups = set.getTextArrayParser("items_group_([0-9]+)")
			val chanceGroups = set.getIntegerArrayParser("chance_group_([0-9]+)")
			
			lootTables = set
					.stream { LootTableItem(it, itemGroups, chanceGroups) }
					.collect(groupingBy(LootTableItem::id))
					.mapValues { it.value.groupBy(LootTableItem::difficulty) }
			
		}
	}
	
	class LootTableItem(set: SdbResultSet, itemGroups: SdbColumnArraySet.SdbTextColumnArraySet, chanceGroups: SdbColumnArraySet.SdbIntegerColumnArraySet) {
		
		val id  = set.getText("loot_id")!!
		val difficulty = set.getText("difficulty")!!
		val minLevel = set.getInt("min_cl").toInt()
		val maxLevel = set.getInt("max_cl").toInt()
		val groups: List<LootGroup>
		
		init {
			val groups = itemGroups.getArray(set)
			val chances = chanceGroups.getArray(set)
			val count = min(groups.size, chances.size)
			
			val groupList = ArrayList<LootGroup>()
			var chance = 0
			for (i in 0 until count) {
				val group = groups[i] ?: continue
				val itemChance = chances[i]
				if (itemChance == 0 || itemChance == Integer.MAX_VALUE)
					continue
				chance += itemChance
				groupList.add(LootGroup(chance, group.split(";").filter { it.isNotBlank() }))
			}
			this.groups = groupList
		}
		
		override fun toString(): String {
			return "LootTableItem(id=$id, difficulty=$difficulty, level=[$minLevel, $maxLevel] groups=$groups)"
		}
		
	}
	
	data class LootGroup(val chance: Int, val items: List<String>)
	
}
