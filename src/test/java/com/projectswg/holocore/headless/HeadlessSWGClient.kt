/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.network.packets.SWGPacket
import com.projectswg.common.network.packets.swg.ErrorMessage
import com.projectswg.common.network.packets.swg.login.LoginClientId
import com.projectswg.common.network.packets.swg.login.LoginClusterStatus
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.resources.support.global.player.Player.PlayerServer
import com.projectswg.holocore.resources.support.global.player.PlayerState
import com.projectswg.holocore.test.resources.GenericPlayer
import java.util.concurrent.TimeUnit

/**
 * A headless SWG client that can be used for testing.
 * Tries the simulate the flow of a real client as much as possible.
 * This class is the entry point for all headless testing.
 *
 * @param username the username of the player
 * @param version optionally, a custom client version to use
 */
class HeadlessSWGClient(private val username: String, private val version: String = "20051010-17:00") {

	val player = GenericPlayer()

	init {
		player.username = username
		player.playerState = PlayerState.CONNECTED
		player.playerServer = PlayerServer.NONE
	}

	fun login(password: String): CharacterSelectionScreen {
		sendPacket(player, LoginClientId(username, password, version))


		val responsePacket = player.waitForNextPacket(
			setOf(LoginClusterStatus::class.java, ErrorMessage::class.java), 50, TimeUnit.MILLISECONDS
		)

		when (responsePacket) {
			is LoginClusterStatus     -> return CharacterSelectionScreen(player)
			is ErrorMessage           -> handleLoginError(responsePacket)
		}

		throw IllegalStateException("Did not receive any known packets in time")
	}

	private fun handleLoginError(errorMessage: ErrorMessage) {
		val message = errorMessage.message

		if (message.lowercase().contains("banned")) {
			throw AccountBannedException(message)
		} else if (message.lowercase().contains("version")) {
			throw WrongClientVersionException(message)
		} else if (message.lowercase().contains("password") || message.lowercase().contains("username")) {
			throw WrongCredentialsException(message)
		} else {
			throw IllegalStateException("Unknown error message: $message")
		}
	}

	override fun toString(): String {
		return "HeadlessSWGClient(player=$player)"
	}

	companion object {

		/**
		 * Convenience method for creating a headless SWG client, logging in, creating a character and zoning it in.
		 */
		fun createZonedInCharacter(username: String, password: String, characterName: String): ZonedInCharacter {
			val swgClient = HeadlessSWGClient(username)
			val characterSelectionScreen = swgClient.login(password)
			val characterId = characterSelectionScreen.createCharacter(characterName)
			return characterSelectionScreen.selectCharacter(characterId)
		}
	}

}

internal fun sendPacket(player: GenericPlayer, packet: SWGPacket) {
	// Currently broadcasts the InboundPacketIntent directly, but this could be changed to send the packet for real
	InboundPacketIntent.broadcast(player, packet)
}
