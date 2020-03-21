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

import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.CreditObject
import java.util.concurrent.ThreadLocalRandom

class CreditLootGenerator() {
	
	fun generate(corpse: CreatureObject, loot: MutableList<SWGObject>) {
		val random = ThreadLocalRandom.current()
		val config = PswgDatabase.config
		
		val range = config.getDouble(this, "lootCashHumanRange", 0.05)
		val cashLootRoll = random.nextDouble()
		
		val credits = corpse.level.toInt() * (1 + random.nextDouble(0.0, range)).toInt() * when (corpse.difficulty) {
			CreatureDifficulty.ELITE -> {
				if (cashLootRoll > config.getDouble(this, "lootCashHumanElitechance", 0.80))
					return
				config.getInt(this, "lootCashHumanElite", 5)
			}
			CreatureDifficulty.BOSS -> // bosses always drop cash loot, so no need to check
				config.getInt(this, "lootCashHumanBoss", 9)
			else -> {
				if (cashLootRoll > config.getDouble(this, "lootCashHumanNormalChance", 0.60))
					return
				config.getInt(this, "lootCashHumanNormal", 2)
			}
		}
		
		// TODO scale with group size?
		
		val cashObject = ObjectCreator.createObjectFromTemplate("object/tangible/item/shared_loot_cash.iff", CreditObject::class.java)
		
		cashObject.amount = credits.toLong()
		loot.add(cashObject)
	}
	
}
