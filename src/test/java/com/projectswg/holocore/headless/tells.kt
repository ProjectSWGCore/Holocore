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
package com.projectswg.holocore.headless

import com.projectswg.common.data.encodables.chat.ChatResult
import com.projectswg.common.network.packets.swg.zone.chat.ChatInstantMessageToCharacter
import com.projectswg.common.network.packets.swg.zone.chat.ChatInstantMessageToClient
import com.projectswg.common.network.packets.swg.zone.chat.ChatOnSendInstantMessage
import kotlin.random.Random

/**
 * Sends a tell to another player.
 * @param characterName the player to send the tell to
 * @param message the message to send
 * @return the result of sending the tell
 */
fun ZonedInCharacter.sendTell(characterName: String, message: String): ChatResult {
	val sequence = Random.nextInt()
	sendPacket(player, ChatInstantMessageToCharacter("Testcase", characterName, message, sequence))
	val chatOnSendInstantMessage = player.waitForNextPacket(ChatOnSendInstantMessage::class.java) ?: throw IllegalStateException("No tell receipt received")
	if (chatOnSendInstantMessage.sequence != sequence) throw IllegalStateException("Invalid sequence. Expected $sequence, got ${chatOnSendInstantMessage.sequence}")
	return ChatResult.fromInteger(chatOnSendInstantMessage.result)
}

/**
 * Waits for a tell to be received. This method will block until a tell is received.
 * @return the tell that was received
 * @throws IllegalStateException if no tell is received
 */
fun ZonedInCharacter.waitForTell(): ReceivedTell {
	val chatInstantMessageToClient = player.waitForNextPacket(ChatInstantMessageToClient::class.java) ?: throw IllegalStateException("No tell received")
	return ReceivedTell(chatInstantMessageToClient.character, chatInstantMessageToClient.message)
}

data class ReceivedTell(val sender: String, val message: String)
