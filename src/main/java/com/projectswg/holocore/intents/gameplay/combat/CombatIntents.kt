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

package com.projectswg.holocore.intents.gameplay.combat

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.Intent

/*
 * Combat events after the fact
 */
data class CreatureIncapacitatedIntent(val incapper: CreatureObject, val incappee: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(incapper: CreatureObject, incapee: CreatureObject) = CreatureIncapacitatedIntent(incapper, incapee).broadcast()
	}
}
data class CreatureKilledIntent(val killer: CreatureObject, val corpse: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(killer: CreatureObject, corpse: CreatureObject) = CreatureKilledIntent(killer, corpse).broadcast()
	}
}
data class CreatureRevivedIntent(val creature: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(creature: CreatureObject) = CreatureRevivedIntent(creature).broadcast()
	}
}
data class EnterCombatIntent(val source: CreatureObject, val target: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(source: CreatureObject, target: CreatureObject) = EnterCombatIntent(source, target).broadcast()
	}
}
data class ExitCombatIntent(val source: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(source: CreatureObject) = ExitCombatIntent(source).broadcast()
	}
}

/*
 * Combat event requests
 */
data class IncapacitateCreatureIntent(val incapper: CreatureObject, val incappee: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(incapper: CreatureObject, incapee: CreatureObject) = IncapacitateCreatureIntent(incapper, incapee).broadcast()
	}
}
data class KillCreatureIntent(val killer: CreatureObject, val corpse: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(killer: CreatureObject, corpse: CreatureObject) = KillCreatureIntent(killer, corpse).broadcast()
	}
}
data class DeathblowIntent(val killer: CreatureObject, val corpse: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(killer: CreatureObject, corpse: CreatureObject) = DeathblowIntent(killer, corpse).broadcast()
	}
}
data class RequestCreatureDeathIntent(val killer: CreatureObject, val corpse: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(killer: CreatureObject, corpse: CreatureObject) = RequestCreatureDeathIntent(killer, corpse).broadcast()
	}
}
