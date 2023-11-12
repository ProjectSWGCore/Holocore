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

import com.projectswg.common.network.packets.swg.login.ClientIdMsg
import com.projectswg.common.network.packets.swg.login.ClientPermissionsMessage
import com.projectswg.common.network.packets.swg.login.creation.ClientCreateCharacter
import com.projectswg.common.network.packets.swg.login.creation.CreateCharacterSuccess
import com.projectswg.common.network.packets.swg.zone.insertion.SelectCharacter
import com.projectswg.holocore.test.resources.GenericPlayer
import java.util.concurrent.TimeUnit

/**
 * Represents everything that happens on the character selection screen.
 */
class CharacterSelectionScreen internal constructor(val player: GenericPlayer) {

	/**
	 * Creates a character belonging to the given player.
	 * @param characterName the name of the character to create
	 * @return the object id of the created character
	 */
	fun createCharacter(characterName: String): Long {
		val clientCreateCharacter = ClientCreateCharacter()
		clientCreateCharacter.biography = ""
		clientCreateCharacter.clothes = "combat_brawler"
		clientCreateCharacter.race = "object/creature/player/shared_human_male.iff"
		clientCreateCharacter.name = characterName
		sendPacket(player, clientCreateCharacter)
		val createCharacterSuccess = player.waitForNextPacket(CreateCharacterSuccess::class.java) ?: throw IllegalStateException("Failed to create character '$characterName' in time")

		return createCharacterSuccess.id
	}

	/**
	 * Selects a character to play as.
	 * @param characterId the object id of the character to select - this is the same as the one returned by [createCharacter]
	 */
	fun selectCharacter(characterId: Long): ZonedInCharacter {
		sendPacket(player, SelectCharacter(characterId))
		sendPacket(player, ClientIdMsg())
		player.waitForNextPacket(ClientPermissionsMessage::class.java, 50, TimeUnit.MILLISECONDS) ?: throw IllegalStateException("Failed to receive client permissions message in time")

		return ZonedInCharacter(player)
	}

	override fun toString(): String {
		return "CharacterSelectionScreen(player=$player)"
	}


}