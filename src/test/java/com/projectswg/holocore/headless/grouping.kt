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

import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService
import java.util.concurrent.TimeUnit

fun ZonedInCharacter.invitePlayerToGroup(other: ZonedInCharacter) {
	sendCommand("invite", other.player.creatureObject)
	player.waitForNextPacket(ChatSystemMessage::class.java, 50, TimeUnit.MILLISECONDS) ?: java.lang.IllegalStateException("No chat system message received")
}

fun ZonedInCharacter.acceptCurrentGroupInvitation() {
	sendCommand("join")
	player.waitForNextPacket(DeltasMessage::class.java, 50, TimeUnit.MILLISECONDS) ?: java.lang.IllegalStateException("Packet not received")
}

fun ZonedInCharacter.leaveCurrentGroup() {
	sendCommand("leaveGroup")
	player.waitForNextPacket(DeltasMessage::class.java, 50, TimeUnit.MILLISECONDS) ?: java.lang.IllegalStateException("Packet not received")
}

fun ZonedInCharacter.kickFromGroup(other: ZonedInCharacter) {
	sendCommand("dismissGroupMember", other.player.creatureObject)
	player.waitForNextPacket(ChatSystemMessage::class.java, 50, TimeUnit.MILLISECONDS) ?: java.lang.IllegalStateException("Packet not received")
}

fun ZonedInCharacter.makeGroupLeader(other: ZonedInCharacter) {
	sendCommand("makeLeader", other.player.creatureObject)
	player.waitForNextPacket(DeltasMessage::class.java, 50, TimeUnit.MILLISECONDS) ?: java.lang.IllegalStateException("Packet not received")
}

fun ZonedInCharacter.isInGroupWith(other: ZonedInCharacter): Boolean {
	val myGroup = player.creatureObject.groupId
	val theirGroup = other.player.creatureObject.groupId
	val ungrouped = myGroup == 0L || theirGroup == 0L
	if (ungrouped)
		return false

	return myGroup == theirGroup
}

fun ZonedInCharacter.isGroupLeader(): Boolean {
	val group = ObjectStorageService.ObjectLookup.getObjectById(player.creatureObject.groupId) as GroupObject? ?: throw IllegalStateException("Player is not in a (known) group")

	return player.creatureObject.objectId == group.leaderId
}

