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

package com.projectswg.holocore.services.gameplay.combat.loot.generation

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.StaticItemCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.IntentChain
import java.util.concurrent.ThreadLocalRandom

class ItemLootGenerator {
	
	fun generate(corpse: CreatureObject, killer: CreatureObject, loot: MutableList<SWGObject>, lootTables: List<NPCLootTable>) {
		val random = ThreadLocalRandom.current()
		
		val tableRoll = random.nextInt(100) + 1
		
		// Admin Variables
		val admin = killer.hasCommand("admin")
		val adminOutput1 = StringBuilder("$tableRoll //")
		val adminOutput2 = StringBuilder()
		
		val lootTableLookup = ServerData.lootTables
		for ((tableId, npcTable) in lootTables.withIndex()) {
			val tableChance = npcTable.chance
			
			if (tableRoll > tableChance) {
				// Skip ahead if there's no drop chance
				adminOutput1.append("/ \\#FF0000 loot_table").append(tableId+1)
				break
			}
			adminOutput1.append("/ \\#00FF00 loot_table").append(tableId+1)
			
			val groupRoll = random.nextInt(100) + 1
			val lootTableItem = lootTableLookup.getLootTableItem(npcTable.lootTable, corpse.difficulty.name, corpse.level.toInt()) ?: continue
			
			for ((chance, items) in lootTableItem.groups) { // group of items to be granted
				if (groupRoll > chance)
					continue
				
				val itemName = items[random.nextInt(items.size)]
				
				adminOutput2.append(itemName).append('\n')
				when {
					itemName.startsWith("dynamic_") -> { // TODO dynamic item handling
						SystemMessageIntent(killer.owner!!, "We don't support this loot item yet: $itemName").broadcast()
					}
					itemName.endsWith(".iff") -> {
						loot.add(ObjectCreator.createObjectFromTemplate(itemName))
					}
					else -> {
						val obj = StaticItemCreator.createItem(itemName)
						if (obj != null)
							loot.add(obj)
					}
				}
				
				break
			}
		}
		if (admin) {
			IntentChain.broadcastChain(
					SystemMessageIntent(killer.owner!!, adminOutput1.toString()),
					SystemMessageIntent(killer.owner!!, adminOutput2.toString())
			)
		}
	}
	
}
