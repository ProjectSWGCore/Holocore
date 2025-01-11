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

import com.projectswg.common.network.packets.swg.zone.SceneDestroyObject
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService
import java.util.concurrent.TimeUnit

fun ZonedInCharacter.invitePlayerToGroup(other: ZonedInCharacter) {
	sendCommand("invite", other.player.creatureObject)
	player.waitForNextPacket(ChatSystemMessage::class.java, 1, TimeUnit.SECONDS) ?: throw IllegalStateException("No chat system message received")
}

fun ZonedInCharacter.acceptCurrentGroupInvitation(from: ZonedInCharacter) {
	sendCommand("join")
	// GroupId is set away from 0
	player.waitForNextObjectDelta(player.creatureObject.objectId, 6, 7, 1, TimeUnit.SECONDS) ?: throw IllegalStateException("Packet not received")
	if (player.creatureObject.groupId == 0L) {
		throw IllegalStateException("Group ID for $player should not still be 0")
	}

	if (from.player.creatureObject.groupId == 0L) {
		// If the group has to be created, we need to pop the DeltasMessage for changing the GroupId of the leader.
		from.player.waitForNextObjectDelta(from.player.creatureObject.objectId, 6, 7, 1, TimeUnit.SECONDS) ?: throw IllegalStateException("Packet not received")
		if (from.player.creatureObject.groupId == 0L) {
			throw IllegalStateException("Group ID for ${from.player} should not still be 0")
		}
	}
}

fun ZonedInCharacter.leaveCurrentGroup() {
	sendCommand("leaveGroup")
	// GroupObject is destroyed
	player.waitForNextPacket(SceneDestroyObject::class.java, 1, TimeUnit.SECONDS) ?: throw IllegalStateException("Packet not received")
}

fun ZonedInCharacter.kickFromGroup(other: ZonedInCharacter) {
	val groupObjectId = player.creatureObject.groupId
	sendCommand("dismissGroupMember", other.player.creatureObject)
	player.waitForNextObjectDelta(groupObjectId, 6, 2, 1, TimeUnit.SECONDS) ?: throw IllegalStateException("Packet not received")
}

fun ZonedInCharacter.makeGroupLeader(other: ZonedInCharacter) {
	val groupObjectId = player.creatureObject.groupId
	sendCommand("makeLeader", other.player.creatureObject)
	if (isGroupLeader()) {
		player.waitForNextObjectDelta(groupObjectId, 6, 2, 1, TimeUnit.SECONDS) ?: throw IllegalStateException("Packet not received")
	} else {
		player.waitForNextPacket(ChatSystemMessage::class.java, 1, TimeUnit.SECONDS) ?: throw IllegalStateException("No chat system message received")
	}
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

fun ZonedInCharacter.groupChat(message: String) {
	sendCommand("groupchat", null, message)
}
