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
package com.projectswg.holocore.services.gameplay.newbie

import com.projectswg.holocore.intents.gameplay.player.quest.GrantQuestIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.global.zone.creation.CreatedCharacterIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

/**
 * Responsible for welcoming created characters to the game by handing out relevant quests.
 */
class NewbieService : Service() {

	private val recentlyCreatedCharacters = mutableSetOf<CreatureObject>()

	@IntentHandler
	private fun handleCreatedCharacterIntent(intent: CreatedCharacterIntent) {
		recentlyCreatedCharacters.add(intent.creatureObject)
	}

	@IntentHandler
	private fun handlePlayerEventIntent(intent: PlayerEventIntent) {
		if (intent.event != PlayerEvent.PE_ZONE_IN_SERVER) {
			return
		}

		val creatureObject = intent.player.creatureObject

		if (creatureObject !in recentlyCreatedCharacters) {
			StandardLog.onPlayerTrace(this, creatureObject, "character not created recently, skipping newbie quest grant")
			return
		}

		val player = intent.player
		GrantQuestIntent.broadcast(player, "quest/c_newbie_start")

		recentlyCreatedCharacters.remove(creatureObject)
	}
}