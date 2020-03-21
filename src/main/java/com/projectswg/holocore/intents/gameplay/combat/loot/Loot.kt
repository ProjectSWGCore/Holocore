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

@file:Suppress("NOTHING_TO_INLINE")

package com.projectswg.holocore.intents.gameplay.combat.loot

import com.projectswg.holocore.resources.gameplay.combat.loot.LootType
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.Intent

data class CorpseLootedIntent(val corpse: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(corpse: CreatureObject) = CorpseLootedIntent(corpse).broadcast()
	}
}
data class LootGeneratedIntent(val corpse: AIObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(corpse: AIObject) = LootGeneratedIntent(corpse).broadcast()
	}
}
/** Requests to transfer a particular item from a corpse */
data class LootItemIntent(val looter: CreatureObject, val corpse: CreatureObject, val item: SWGObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(looter: CreatureObject, corpse: CreatureObject, item: SWGObject) = LootItemIntent(looter, corpse, item).broadcast()
	}
}
data class LootLotteryStartedIntent(val corpse: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(corpse: CreatureObject) = LootLotteryStartedIntent(corpse).broadcast()
	}
}
/** Requests a particular high-level action on a corpse */
data class LootRequestIntent(val player: Player, val target: CreatureObject, val type: LootType): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(player: Player, target: CreatureObject, type: LootType) = LootRequestIntent(player, target, type).broadcast()
	}
}
data class OpenRareChestIntent(val actor: CreatureObject, val chest: TangibleObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(actor: CreatureObject, chest: TangibleObject) = OpenRareChestIntent(actor, chest).broadcast()
	}
}
