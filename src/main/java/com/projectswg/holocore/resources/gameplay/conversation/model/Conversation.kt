/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.conversation.model

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.holocore.resources.support.global.player.Player

class Conversation(val id: String) {
	var npcMessage: ProsePackage? = null
	private val playerResponses: MutableList<PlayerResponse> = ArrayList()
	private val events: MutableList<Event> = ArrayList()
	private val requirements: MutableList<Requirement> = ArrayList()

	fun addPlayerResponse(playerResponse: PlayerResponse) {
		playerResponses.add(playerResponse)
	}

	fun addEvent(event: Event) {
		events.add(event)
	}

	fun addRequirement(requirement: Requirement) {
		requirements.add(requirement)
	}

	fun getPlayerResponses(): List<PlayerResponse> {
		return playerResponses
	}

	fun isAllowed(player: Player): Boolean {
		for (requirement in requirements) {
			val available = requirement.test(player)

			if (!available) {
				return false
			}
		}

		return true
	}

	fun getEvents(): List<Event> {
		return events
	}

	override fun toString(): String {
		return "Conversation{id='$id'}"
	}
}
