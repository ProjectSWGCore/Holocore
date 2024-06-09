/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.gameplay.conversation.events

import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionIntent
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionStatusIntent
import com.projectswg.holocore.resources.gameplay.conversation.model.Event
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.factions
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import me.joshlarson.jlcommon.log.Log

class ChangePlayerFactionEvent(private val factionName: String) : Event {
	override fun trigger(player: Player, npc: AIObject) {
		val faction = factions.getFaction(factionName)
		if (faction == null) {
			Log.w("ChangePlayerFactionEvent: Invalid faction name: %s", factionName)
			return
		}

		UpdateFactionIntent(player.creatureObject, faction).broadcast()
		UpdateFactionStatusIntent(player.creatureObject, PvpStatus.COMBATANT).broadcast()
	}
}
