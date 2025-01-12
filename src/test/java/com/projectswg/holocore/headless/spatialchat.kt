/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.headless

import com.projectswg.common.network.packets.swg.zone.object_controller.SpatialChat

/**
 * Sends a spatial chat message.
 * @param message the message to send
 * @param targetId the target that the message is optionally directed at
 * @param chatType the type of chat message
 * @param moodId the mood of the chat message, like saying something angrily
 * @param languageId the language of the chat message - Basic, Shyriiwook, etc.
 */
fun ZonedInCharacter.sendSpatialChat(message: String, targetId: Long = 0, chatType: Int = 0, moodId: Int = 0, languageId: Int = 0) {
	sendCommand("spatialChatInternal", null, "$targetId $chatType $moodId unknown $languageId $message")
}

fun ZonedInCharacter.waitForSpatialChat(): ReceivedSpatialChat {
	val spatialChat = player.waitForNextPacket(SpatialChat::class.java) ?: throw NoSpatialChatReceivedException()
	return ReceivedSpatialChat(spatialChat.sourceId, spatialChat.text)
}

data class ReceivedSpatialChat(val sourceId: Long, val message: String)

class NoSpatialChatReceivedException : IllegalStateException("No spatial chat received")
