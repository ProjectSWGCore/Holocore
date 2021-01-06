/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.intents.gameplay.conversation

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import me.joshlarson.jlcommon.control.Intent

data class StartConversationIntent(val starter: CreatureObject, val npc: AIObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(starter: CreatureObject, npc: AIObject) = StartConversationIntent(starter, npc).broadcast()
	}
}

data class ProgressConversationIntent(val starter: CreatureObject, val selection: Int): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(starter: CreatureObject, selection: Int) = ProgressConversationIntent(starter, selection).broadcast()
	}
}

data class StopConversationIntent(val creatureObject: CreatureObject): Intent() {
	companion object {
		@JvmStatic inline fun broadcast(creatureObject: CreatureObject) = StopConversationIntent(creatureObject).broadcast()
	}
}