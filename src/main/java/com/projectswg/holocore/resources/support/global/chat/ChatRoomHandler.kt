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
package com.projectswg.holocore.resources.support.global.chat

import com.projectswg.common.data.encodables.chat.ChatAvatar
import com.projectswg.common.data.encodables.chat.ChatResult
import com.projectswg.common.data.encodables.chat.ChatRoom
import com.projectswg.common.data.encodables.oob.OutOfBandPackage
import com.projectswg.common.network.packets.SWGPacket
import com.projectswg.common.network.packets.swg.zone.chat.*
import com.projectswg.common.network.packets.swg.zone.insertion.ChatRoomList
import com.projectswg.holocore.ProjectSWG.galaxy
import com.projectswg.holocore.resources.support.data.server_info.loader.DefaultChatRoom
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.defaultChatRooms
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.planetChatRooms
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.chatRooms
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.stream.Collectors

class ChatRoomHandler {
	private val database = chatRooms
	private val rooms = ChatRoomContainer()
	private val maxChatRoomId = AtomicInteger(0)
	private val roomCreationMutex = Any()

	fun initialize(): Boolean {
		database.getChatRooms().forEach(Consumer { room: ChatRoom ->
			if (room.id >= maxChatRoomId.get()) maxChatRoomId.set(room.id)
			if (room.owner == ChatAvatar.systemAvatar) return@Consumer
			rooms.addRoom(room)
		})
		createSystemChannels()
		return true
	}

	fun terminate(): Boolean {
		return true
	}

	fun enterChatChannels(player: Player) {
		for (channel in player.playerObject.getJoinedChannels()) {
			enterChatChannel(player, channel, false)
		}

		if (player.accessLevel != AccessLevel.PLAYER) {
			for (room in AdminChatRooms.entries) {
				enterChatChannel(player, room.roomPath, true)
			}
		}
	}

	/**
	 * Attempts to join the specified chat channel
	 *
	 * @param player Player joining the chat channel
	 * @param room Chat room to enter
	 */
	fun enterChatChannel(player: Player, room: ChatRoom, sequence: Int, ignoreInvitation: Boolean) {
		val avatar = ChatAvatar(player.characterChatName)

		var result = room.canJoinRoom(avatar, ignoreInvitation)
		if (player.accessLevel != AccessLevel.PLAYER) result = ChatResult.SUCCESS

		if (result != ChatResult.SUCCESS) {
			player.sendPacket(ChatOnEnteredRoom(avatar, result, room.id, sequence))
			return
		}

		// TODO: Check if player is appropriate faction for the room (Rebel and imperial chat rooms)

		// Server-based list so we can join chat channels automatically
		player.playerObject.addJoinedChannel(room.path)


		// Re-send the player the room list with just this room as it could have been public/hidden
		// This also "refreshes" the client, not sending this will cause a Chat channel unavailable message.
		// if (!room.isPublic())
		player.sendPacket(ChatRoomList(room))


		// Notify players of success, it's ChatResult.SUCCESS at this point
		player.sendPacket(ChatOnEnteredRoom(avatar, result, room.id, sequence))


		// Notify everyone that a player entered the room
		sendPacketToMembers(room, ChatOnEnteredRoom(avatar, result, room.id, 0))

		room.addMember(avatar)
	}

	fun enterChatChannel(player: Player, id: Int, sequence: Int) {
		val room = rooms.getRoomById(id)
		if (room == null) {
			player.sendPacket(ChatOnEnteredRoom(ChatAvatar(player.characterChatName), ChatResult.NONE, id, sequence))
			return
		}
		enterChatChannel(player, room, sequence, false)
	}

	fun enterChatChannel(player: Player, path: String, ignoreInvitation: Boolean) {
		val room = rooms.getRoomByPath(path)
		if (room == null) {
			// Channel was not found, attempt to remove it from this players list of channels if it exists.
			// This can happen if a channel was deleted while the player was offline
			player.playerObject.removeJoinedChannel(path)
			return
		}

		enterChatChannel(player, room, 0, ignoreInvitation)
	}

	fun leaveChatChannels(player: Player) {
		val playerObject = player.playerObject
		val joinedChannels = playerObject.getJoinedChannels()

		for (joinedChannel in joinedChannels) {
			leaveChatChannel(player, joinedChannel)
		}
	}

	fun leaveChatChannel(player: Player, room: ChatRoom, sequence: Int) {
		val avatar = ChatAvatar(player.characterChatName)

		if (!room.removeMember(avatar) && !player.playerObject.removeJoinedChannel(room.path)) return

		player.sendPacket(ChatOnLeaveRoom(avatar, ChatResult.SUCCESS.code, room.id, sequence))
		sendPacketToMembers(room, ChatOnLeaveRoom(avatar, ChatResult.SUCCESS.code, room.id, 0))
	}

	fun leaveChatChannel(player: Player, path: String) {
		val room = rooms.getRoomByPath(path) ?: return
		leaveChatChannel(player, room, 0)
	}

	/**
	 * Creates a new chat room with the specified address path. If the path's parent channel doesn't exist, then a new chat room is created with the same passed arguments.
	 *
	 * @param creator Room creator who will also become the owner of this room
	 * @param isPublic Determines if the room should be publicly displayed in the channel listing
	 * @param moderated Determines if the room should be moderated
	 * @param path Address for the channel (Ex: SWG.serverName.Imperial)
	 * @param title Descriptive name of the chat channel (Ex: Imperial chat for this galaxy)
	 * @param persist If true then this channel will be saved in a persistent data store
	 * @return TRUE if the room was successfully created, FALSE otherwise
	 */
	fun createRoom(creator: ChatAvatar, isPublic: Boolean, moderated: Boolean, path: String, title: String, persist: Boolean): Boolean {
		assert(path.isNotEmpty()) { "path must be non-empty" }
		assert(path.startsWith("SWG." + creator.galaxy) && path != "SWG." + creator.galaxy) { "Invalid path! $path" }

		synchronized(roomCreationMutex) {
			if (rooms.getRoomByPath(path) != null) return false
			// All paths should have parents, lets validate to make sure they exist first. Create them if they don't.
			// This chunk of code makes this function recursive
			val lastIndex = path.lastIndexOf('.')
			if (lastIndex != -1) {
				val parentPath = path.substring(0, lastIndex)
				if (parentPath != "SWG." + creator.galaxy) createRoom(creator, isPublic, false, parentPath, "", persist)
			}

			val room = ChatRoom()
			room.id = maxChatRoomId.incrementAndGet()
			room.owner = creator
			room.creator = creator
			room.isPublic = isPublic
			room.isModerated = moderated
			room.path = path
			room.title = title
			room.addModerator(creator)
			rooms.addRoom(room)

			if (persist) database.addChatRoom(room)
			return true
		}
	}

	fun sendMessageToRoom(player: Player, roomId: Int, sequence: Int, message: String, oobPackage: OutOfBandPackage?) {
		sendMessageToRoom(player, rooms.getRoomById(roomId), sequence, message, oobPackage)
	}

	fun sendMessageToRoom(player: Player, path: String, sequence: Int, message: String, oobPackage: OutOfBandPackage?) {
		sendMessageToRoom(player, rooms.getRoomByPath(path), sequence, message, oobPackage)
	}

	fun sendMessageToRoom(player: Player, room: ChatRoom?, sequence: Int, message: String, oobPackage: OutOfBandPackage?) {
		if (room == null) {
			player.sendPacket(ChatOnSendRoomMessage(ChatResult.ROOM_INVALID_ID.code, sequence))
			return
		}

		val avatar = ChatAvatar(player.characterChatName)
		var result = room.canSendMessage(avatar)
		if (result == ChatResult.SUCCESS && message.length > 512) result = ChatResult.ROOM_AVATAR_NO_PERMISSION

		player.sendPacket(ChatOnSendRoomMessage(result.code, sequence))

		if (result == ChatResult.SUCCESS) {
			sendMessage(room, avatar, message, oobPackage)
		}
	}

	fun sendMessageToRoomFromSystem(path: String, message: String, oobPackage: OutOfBandPackage?) {
		val systemAvatar = ChatAvatar.systemAvatar
		val room = rooms.getRoomByPath(path)

		sendMessage(room, systemAvatar, message, oobPackage)
	}

	fun notifyDestroyRoom(destroyer: ChatAvatar, roomPath: String, sequence: Int): Boolean {
		var room: ChatRoom?

		synchronized(roomCreationMutex) {
			room = rooms.getRoomByPath(roomPath)
			if (room == null) return false
			rooms.destroyRoom(room!!)
		}

		// Send the ChatOnDestroyRoom SWGPacket to every else in the room besides the person destroying the SWGPacket
		val packet = ChatOnDestroyRoom(destroyer, ChatResult.SUCCESS.code, room!!.id, 0)
		room!!.members.forEach(Consumer { member: ChatAvatar ->
			if (destroyer != member) getPlayer(member)!!.sendPacket(packet)
		})

		return true
	}

	private fun createSystemChannels() {
		/**
		 * Channel Notes
		 * Group channels: created by System
		 * SWG.serverName.group.GroupObjectId.GroupChat title = GroupId
		 * Guild channels: created by System
		 * SWG.serverName.guild.GuildId.GuildChat title = GuildId
		 * City channels: created by System
		 * SWG.serverName.city.CityId.CityChat title = CityId
		 */

		val galaxy = galaxy.name
		val systemAvatar = ChatAvatar.Companion.systemAvatar
		val basePath = "SWG.$galaxy."

		createDefaultChannels(systemAvatar, basePath)
		createPlanetChannels(systemAvatar, basePath)
		createAdminChannels(systemAvatar)
	}

	private fun createDefaultChannels(systemAvatar: ChatAvatar, basePath: String) {
		defaultChatRooms.getAll().forEach(Consumer { defaultChatRoom: DefaultChatRoom ->
			val roomName = defaultChatRoom.name
			val roomTitle = defaultChatRoom.title
			createRoom(systemAvatar, isPublic = true, moderated = false, path = basePath + roomName, title = roomTitle, persist = false)
		})
	}

	private fun createPlanetChannels(systemAvatar: ChatAvatar, basePath: String) {
		val planetChatRooms = planetChatRooms
		planetChatRooms.planetNames.forEach(Consumer { planetName: String ->
			val path = "$basePath$planetName."
			createRoom(systemAvatar, isPublic = true, moderated = false, path = path + "Planet", title = "public chat for this planet, cannot create rooms here", persist = false)
			createRoom(systemAvatar, isPublic = true, moderated = false, path = path + "system", title = "system messages for this planet, cannot create rooms here", persist = false)
			createRoom(systemAvatar, isPublic = true, moderated = false, path = path + "Chat", title = "public chat for this planet, can create rooms here", persist = false)
		})
	}

	private fun createAdminChannels(systemAvatar: ChatAvatar) {
		for (room in AdminChatRooms.entries) {
			createRoom(systemAvatar, isPublic = false, moderated = false, path = room.roomPath, title = room.roomTitle, persist = false)
		}
	}

	fun getRoomById(roomId: Int): ChatRoom? {
		return rooms.getRoomById(roomId)
	}

	fun getRoomByPath(path: String): ChatRoom? {
		return rooms.getRoomByPath(path)
	}

	fun getRoomList(player: Player): List<ChatRoom?> {
		val avatar = ChatAvatar(player.characterChatName)

		val admin = player.accessLevel.value >= AccessLevel.CSR.value
		return rooms.allRooms.stream().filter { r: ChatRoom? -> r!!.isPublic || r.isInvited(avatar) || r.owner == avatar || admin }.collect(Collectors.toList())
	}

	companion object {
		private fun sendMessage(room: ChatRoom?, sender: ChatAvatar, message: String, oob: OutOfBandPackage?) {
			var message = message
			if (message.startsWith("\\#")) message = " $message"
			val chatRoomMessage = ChatRoomMessage(sender, room!!.id, message, oob)
			for (member in room.members) {
				val player = getPlayer(member) ?: continue
				val playerObject = player.playerObject
				if (playerObject != null && playerObject.isIgnored(sender.name)) continue

				player.sendPacket(chatRoomMessage)
			}
		}

		private fun sendPacketToMembers(room: ChatRoom, packet: SWGPacket) {
			room.members.stream().map { avatar: ChatAvatar -> getPlayer(avatar) }.filter { obj: Player? -> Objects.nonNull(obj) } // Don't try sending packets to players that can't be found
				.forEach { player: Player? -> player!!.sendPacket(packet) }
		}

		private fun getPlayer(avatar: ChatAvatar): Player? {
			return PlayerLookup.getPlayerByFirstName(avatar.name)
		}
	}
}
