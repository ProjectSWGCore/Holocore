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
package com.projectswg.holocore.intents.support.global.chat

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType
import com.projectswg.holocore.resources.support.global.player.Player
import me.joshlarson.jlcommon.control.Intent

class SystemMessageIntent : Intent {
	val broadcastType: BroadcastType
	val receiver: Player?
	val terrain: Terrain?
	val message: String?
	val prose: ProsePackage?
	val systemChatType: SystemChatType

	/**
	 * Custom broadcast type with a specified receiver and message
	 *
	 * @param receiver the receiver to send to
	 * @param message the message to send
	 * @param type the broadcast type
	 */
	constructor(receiver: Player, message: String, type: BroadcastType) {
		this.broadcastType = type
		this.receiver = receiver
		this.terrain = null
		this.message = message
		this.prose = null
		this.systemChatType = SystemChatType.PERSONAL
	}

	/**
	 * Planet-wide message to the specified terrain with the message
	 *
	 * @param terrain the terrain to broadcast on
	 * @param message the message to send
	 */
	constructor(terrain: Terrain?, message: String?) {
		this.broadcastType = BroadcastType.PLANET
		this.receiver = null
		this.terrain = terrain
		this.message = message
		this.prose = null
		this.systemChatType = SystemChatType.PERSONAL
	}

	/**
	 * Personal message to the receiver with the prose package
	 *
	 * @param receiver the receiver
	 * @param prose the prose package to send
	 */
	constructor(receiver: Player, prose: ProsePackage) {
		this.broadcastType = BroadcastType.PERSONAL
		this.receiver = receiver
		this.terrain = null
		this.message = null
		this.prose = prose
		this.systemChatType = SystemChatType.PERSONAL
	}

	/**
	 * Personal message to the receiver with the prose package
	 *
	 * @param receiver the receiver
	 * @param prose the prose package to send
	 */
	constructor(receiver: Player, prose: ProsePackage, systemChatType: SystemChatType) {
		this.broadcastType = BroadcastType.PERSONAL
		this.receiver = receiver
		this.terrain = null
		this.message = null
		this.prose = prose
		this.systemChatType = systemChatType
	}

	/**
	 * Personal message to the receiver with the message
	 *
	 * @param receiver the receiver
	 * @param message the message
	 */
	constructor(receiver: Player, message: String) {
		this.broadcastType = BroadcastType.PERSONAL
		this.receiver = receiver
		this.terrain = null
		this.message = message
		this.prose = null
		this.systemChatType = SystemChatType.PERSONAL
	}

	/**
	 * @param message the message
	 * @param type the broadcast type
	 */
	constructor(message: String, type: BroadcastType) {
		this.broadcastType = type
		this.receiver = null
		this.terrain = null
		this.message = message
		this.prose = null
		this.systemChatType = SystemChatType.PERSONAL
	}

	enum class BroadcastType {
		AREA,
		PLANET,
		GALAXY,
		PERSONAL
	}

	companion object {
		fun broadcastPersonal(receiver: Player, message: String) {
			SystemMessageIntent(receiver, message).broadcast()
		}

		fun broadcastPersonal(receiver: Player, prose: ProsePackage) {
			SystemMessageIntent(receiver, prose, SystemChatType.PERSONAL).broadcast()
		}

		fun broadcastPersonal(receiver: Player, prose: ProsePackage, systemChatType: SystemChatType) {
			SystemMessageIntent(receiver, prose, systemChatType).broadcast()
		}

		fun broadcastArea(receiver: Player, message: String) {
			SystemMessageIntent(receiver, message, BroadcastType.AREA).broadcast()
		}

		fun broadcastPlanet(terrain: Terrain, message: String) {
			SystemMessageIntent(terrain, message).broadcast()
		}

		fun broadcastGalaxy(message: String) {
			SystemMessageIntent(message, BroadcastType.GALAXY).broadcast()
		}
	}
}
