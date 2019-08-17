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

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.gameplay.combat.loot.RareLootService.*
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs

class RareItemLootGenerator {
	
	fun generate(corpse: CreatureObject, killer: CreatureObject, loot: MutableList<SWGObject>) {
		if (!isEligibleForRareLoot(corpse.level.toInt(), killer.level.toInt()))
			return
		
		val roll = ThreadLocalRandom.current().nextInt(100) + 1    // Rolls from 0 to 99, then we add 1 and it becomes 1 to 100
		if (roll >= DROP_CHANCE) {
			Log.d("No RLS drop from %s with roll %d", corpse, roll)
			return
		}
		
		val template = templateForDifficulty(corpse.difficulty)
		val chest = ObjectCreator.createObjectFromTemplate(template)
		
		chest.setStf("loot_n", chestIdForTemplate(template) + "_n")
		chest.setDetailStf(StringId("loot_n", chestIdForTemplate(template) + "_d"))    // Not located in loot_d, for whatever reason...
		
		loot.add(chest)
	}
	
	private fun isEligibleForRareLoot(corpseLevel: Int, killerLevel: Int): Boolean {
		return abs(corpseLevel - killerLevel) <= MAX_LEVEL_DIFFERENCE || (killerLevel >= 85 && corpseLevel >= 90)
	}
	
	private fun chestIdForTemplate(template: String): String {
		return template.replace("object/tangible/item/shared_", "").replace(".iff", "")
	}
	
	private fun templateForDifficulty(difficulty: CreatureDifficulty): String {
		return when (difficulty) {
			CreatureDifficulty.NORMAL -> RARE_CHEST
			CreatureDifficulty.ELITE  -> EXCEPTIONAL_CHEST
			CreatureDifficulty.BOSS   -> LEGENDARY_CHEST
		}
	}
	
	companion object {
		
		// TODO these two could be config options
		private const val MAX_LEVEL_DIFFERENCE: Short = 6    // +-6 difference is allowed between killer and corpse
		private const val DROP_CHANCE = 1    // One in a hundred eligible kills will drop a chest
		
	}
}
