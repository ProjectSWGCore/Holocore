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

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueDequeue
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.test.resources.GenericPlayer
import java.util.concurrent.TimeUnit

/**
 * Represents everything that can happen to a character that is zoned in.
 */
class ZonedInCharacter internal constructor(val player: GenericPlayer) {

	internal fun sendCommand(command: String, target: SWGObject? = null, args: String = "") {
		val targetObjectId = target?.objectId ?: 0
		val commandQueueEnqueue = CommandQueueEnqueue(player.creatureObject.objectId, 0, CRC.getCrc(command.lowercase()), targetObjectId, args)
		sendPacket(player, commandQueueEnqueue)
		val packet = player.waitForNextPacket(CommandQueueDequeue::class.java, 80, TimeUnit.MILLISECONDS) ?: throw IllegalStateException("Failed to receive dequeue for command '$command' in time")
		if (packet.error != CommandQueueDequeue.ErrorCode.SUCCESS) {
			throw CommandFailedException("Command '$command' failed: ${packet.error}")
		}
	}

	override fun toString(): String {
		return "ZonedInCharacter(player=$player)"
	}
}
