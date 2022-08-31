/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.chat;

import com.projectswg.common.data.encodables.chat.ChatAvatar;
import com.projectswg.common.data.encodables.chat.ChatResult;
import com.projectswg.common.data.encodables.chat.ChatRoom;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.chat.*;
import com.projectswg.common.network.packets.swg.zone.insertion.ChatRoomList;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.resources.support.data.client_info.ServerFactory;
import com.projectswg.holocore.resources.support.data.server_info.CachedObjectDatabase;
import com.projectswg.holocore.resources.support.data.server_info.ObjectDatabase;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ChatRoomHandler {
	
	private final ObjectDatabase<ChatRoom> database;
	private final ChatRoomContainer rooms;
	private final AtomicInteger maxChatRoomId;
	private final Object roomCreationMutex;
	
	public ChatRoomHandler() {
		this.database = new CachedObjectDatabase<>("odb/chat_rooms.db", ChatRoom::create, ChatRoom::save);
		this.rooms = new ChatRoomContainer();
		this.maxChatRoomId = new AtomicInteger(0);
		this.roomCreationMutex = new Object();
	}
	
	public boolean initialize() {
		database.load();
		database.traverse((room) -> {
			if (room.getId() >= maxChatRoomId.get())
				maxChatRoomId.set(room.getId());
			if (room.getOwner().equals(ChatAvatar.getSystemAvatar()))
				return;
			rooms.addRoom(room);
		});
		createSystemChannels();
		return true;
	}
	
	public boolean terminate() {
		database.save();
		database.close();
		return true;
	}
	
	public void enterChatChannels(Player player) {
		for (String channel : player.getPlayerObject().getJoinedChannels()) {
			enterChatChannel(player, channel, false);
		}

		if (player.getAccessLevel() != AccessLevel.PLAYER) {
			for (AdminChatRooms room : AdminChatRooms.values()) {
				enterChatChannel(player, room.getRoomPath(), true);
			}
		}
	}
	
	/**
	 * Attempts to join the specified chat channel
	 * 
	 * @param player Player joining the chat channel
	 * @param room Chat room to enter
	 */
	public void enterChatChannel(Player player, ChatRoom room, int sequence, boolean ignoreInvitation) {
		ChatAvatar avatar = new ChatAvatar(player.getCharacterChatName());
		
		ChatResult result = room.canJoinRoom(avatar, ignoreInvitation);
		if (player.getAccessLevel() != AccessLevel.PLAYER)
			result = ChatResult.SUCCESS;
		
		if (result != ChatResult.SUCCESS) {
			player.sendPacket(new ChatOnEnteredRoom(avatar, result, room.getId(), sequence));
			return;
		}
		// TODO: Check if player is appropriate faction for the room (Rebel and imperial chat rooms)
		
		// Server-based list so we can join chat channels automatically
		player.getPlayerObject().addJoinedChannel(room.getPath());
		
		// Re-send the player the room list with just this room as it could have been public/hidden
		// This also "refreshes" the client, not sending this will cause a Chat channel unavailable message.
		// if (!room.isPublic())
		player.sendPacket(new ChatRoomList(room));
		
		// Notify players of success, it's ChatResult.SUCCESS at this point
		player.sendPacket(new ChatOnEnteredRoom(avatar, result, room.getId(), sequence));
		
		// Notify everyone that a player entered the room
		sendPacketToMembers(room, new ChatOnEnteredRoom(avatar, result, room.getId(), 0));
		
		room.addMember(avatar);
	}
	
	public void enterChatChannel(Player player, int id, int sequence) {
		ChatRoom room = rooms.getRoomById(id);
		if (room == null) {
			player.sendPacket(new ChatOnEnteredRoom(new ChatAvatar(player.getCharacterChatName()), ChatResult.NONE, id, sequence));
			return;
		}
		enterChatChannel(player, room, sequence, false);
	}
	
	public void enterChatChannel(Player player, String path, boolean ignoreInvitation) {
		ChatRoom room = rooms.getRoomByPath(path);
		if (room == null) {
			// Channel was not found, attempt to remove it from this players list of channels if it exists.
			// This can happen if a channel was deleted while the player was offline
			player.getPlayerObject().removeJoinedChannel(path);
			return;
		}
		
		enterChatChannel(player, room, 0, ignoreInvitation);
	}
	
	public void leaveChatChannels(Player player) {
		PlayerObject playerObject = player.getPlayerObject();
		List<String> joinedChannels = playerObject.getJoinedChannels();
		
		for (String joinedChannel : joinedChannels) {
			leaveChatChannel(player, joinedChannel);
		}
	}
	
	public void leaveChatChannel(Player player, ChatRoom room, int sequence) {
		ChatAvatar avatar = new ChatAvatar(player.getCharacterChatName());
		
		if (!room.removeMember(avatar) && !player.getPlayerObject().removeJoinedChannel(room.getPath()))
			return;
		
		player.sendPacket(new ChatOnLeaveRoom(avatar, ChatResult.SUCCESS.getCode(), room.getId(), sequence));
		sendPacketToMembers(room, new ChatOnLeaveRoom(avatar, ChatResult.SUCCESS.getCode(), room.getId(), 0));
	}
	
	public void leaveChatChannel(Player player, String path) {
		ChatRoom room = rooms.getRoomByPath(path);
		if (room == null)
			return;
		leaveChatChannel(player, room, 0);
	}
	
	/**
	 * Creates a new chat room with the specified address path. If the path's parent channel doesn't exist, then a new chat room is created with the same passed arguments.
	 * 
	 * @param creator Room creator who will also become the owner of this room
	 * @param isPublic Determines if the room should be publicly displayed in the channel listing
	 * @param moderated Determines if the room should be moderated
	 * @param path Address for the channel (Ex: SWG.serverName.Imperial)
	 * @param title Descriptive name of the chat channel (Ex: Imperial chat for this galaxy)
	 * @param persist If true then this channel will be saved in an {@link ObjectDatabase}
	 * @return TRUE if the room was successfully created, FALSE otherwise
	 */
	public boolean createRoom(@NotNull ChatAvatar creator, boolean isPublic, boolean moderated, @NotNull String path, @NotNull String title, boolean persist) {
		assert !path.isEmpty() : "path must be non-empty";
		assert path.startsWith("SWG."+creator.getGalaxy()) && !path.equals("SWG."+creator.getGalaxy()) : "Invalid path! " + path;
		
		synchronized (roomCreationMutex) {
			if (rooms.getRoomByPath(path) != null)
				return false;
			
			// All paths should have parents, lets validate to make sure they exist first. Create them if they don't.
			// This chunk of code makes this function recursive
			int lastIndex = path.lastIndexOf('.', path.length());
			if (lastIndex != -1) {
				String parentPath = path.substring(0, lastIndex);
				if (!parentPath.equals("SWG."+creator.getGalaxy()))
					createRoom(creator, isPublic, false, parentPath, "", persist);
			}
			
			ChatRoom room = new ChatRoom();
			room.setId(maxChatRoomId.incrementAndGet());
			room.setOwner(creator);
			room.setCreator(creator);
			room.setIsPublic(isPublic);
			room.setModerated(moderated);
			room.setPath(path);
			room.setTitle(title);
			room.addModerator(creator);
			rooms.addRoom(room);
			
			if (persist)
				database.add(room);
			
			return true;
		}
	}
	
	public void sendMessageToRoom(Player player, int roomId, int sequence, String message, OutOfBandPackage oobPackage) {
		sendMessageToRoom(player, rooms.getRoomById(roomId), sequence, message, oobPackage);
	}
	
	public void sendMessageToRoom(Player player, String path, int sequence, String message, OutOfBandPackage oobPackage) {
		sendMessageToRoom(player, rooms.getRoomByPath(path), sequence, message, oobPackage);
	}
	
	public void sendMessageToRoom(Player player, ChatRoom room, int sequence, String message, OutOfBandPackage oobPackage) {
		if (room == null) {
			player.sendPacket(new ChatOnSendRoomMessage(ChatResult.ROOM_INVALID_ID.getCode(), sequence));
			return;
		}
		
		ChatAvatar avatar = new ChatAvatar(player.getCharacterChatName());
		ChatResult result = room.canSendMessage(avatar);
		if (result == ChatResult.SUCCESS && message.length() > 512)
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		
		player.sendPacket(new ChatOnSendRoomMessage(result.getCode(), sequence));
		
		if (result == ChatResult.SUCCESS) {
			sendMessage(room, avatar, message, oobPackage);
		}
	}

	public void sendMessageToRoomFromSystem(String path, String message, OutOfBandPackage oobPackage) {
		ChatAvatar systemAvatar = ChatAvatar.getSystemAvatar();
		ChatRoom room = rooms.getRoomByPath(path);

		sendMessage(room, systemAvatar, message, oobPackage);
	}

	public boolean notifyDestroyRoom(ChatAvatar destroyer, String roomPath, int sequence) {
		ChatRoom room;
		
		synchronized (roomCreationMutex) {
			room = rooms.getRoomByPath(roomPath);
			if (room == null)
				return false;
			rooms.destroyRoom(room);
		}
		
		// Send the ChatOnDestroyRoom SWGPacket to every else in the room besides the person destroying the SWGPacket
		ChatOnDestroyRoom SWGPacket = new ChatOnDestroyRoom(destroyer, ChatResult.SUCCESS.getCode(), room.getId(), 0);
		room.getMembers().forEach(member -> {
			if (!destroyer.equals(member))
				getPlayer(member).sendPacket(SWGPacket);
		});
		
		return true;
	}
	
	private void createSystemChannels() {
		/**
		 * Channel Notes
		 *   Group channels: created by System
		 *      SWG.serverName.group.GroupObjectId.GroupChat title = GroupId
		 *   Guild channels: created by System
		 *      SWG.serverName.guild.GuildId.GuildChat title = GuildId
		 *   City channels: created by System
		 *      SWG.serverName.city.CityId.CityChat title = CityId
		 */
		
		String galaxy = ProjectSWG.getGalaxy().getName();
		ChatAvatar systemAvatar = ChatAvatar.getSystemAvatar();
		String basePath = "SWG." + galaxy + '.';
		
		DatatableData rooms = ServerFactory.getDatatable("chat/default_rooms.iff");
		rooms.handleRows((r) -> createRoom(systemAvatar, true, false, basePath + rooms.getCell(r, 0), (String) rooms.getCell(r, 1), false));
		
		createPlanetChannels(systemAvatar, basePath);
		createAdminChannels(systemAvatar, basePath);
		
		/*
		 * Battlefield Room path examples: SWG.Bria.corellia.battlefield SWG.Bria.corellia.battlefield.corellia_mountain_fortress.allFactions SWG.Bria.corellia.battlefield.corellia_pvp.allFactions / Imperial / Rebel SWG.Bria.corellia.battlefield.corellia_rebel_riverside_fort.allFactions
		 */
	}
	
	private void createPlanetChannels(ChatAvatar systemAvatar, String basePath) {
		DatatableData planets = ServerFactory.getDatatable("chat/planets.iff");
		planets.handleRows((r) -> {
			String path = basePath + planets.getCell(r, 0) + '.';
			createRoom(systemAvatar, true, false, path + "Planet", "public chat for this planet, cannot create rooms here", false);
			createRoom(systemAvatar, true, false, path + "system", "system messages for this planet, cannot create rooms here", false);
			createRoom(systemAvatar, true, false, path + "Chat", "public chat for this planet, can create rooms here", false);
		});
	}

	private void createAdminChannels(ChatAvatar systemAvatar, String basePath) {
		for (AdminChatRooms room : AdminChatRooms.values()) {
			createRoom(systemAvatar, false, false, room.getRoomPath(), room.getRoomTitle(), false);
		}
	}
	
	public ChatRoom getRoomById(int roomId) {
		return rooms.getRoomById(roomId);
	}
	
	@Nullable
	public ChatRoom getRoomByPath(String path) {
		return rooms.getRoomByPath(path);
	}
	
	public List<ChatRoom> getRoomList(Player player) {
		ChatAvatar avatar = new ChatAvatar(player.getCharacterChatName());
		
		boolean admin = player.getAccessLevel().getValue() >= AccessLevel.CSR.getValue();
		return rooms.getAllRooms().stream()
				.filter(r -> r.isPublic() || r.isInvited(avatar) || r.getOwner().equals(avatar) || admin)
				.collect(Collectors.toList());
	}
	
	private static void sendMessage(ChatRoom room, ChatAvatar sender, String message, OutOfBandPackage oob) {
		if (message.startsWith("\\#"))
			message = " " + message;
		ChatRoomMessage chatRoomMessage = new ChatRoomMessage(sender, room.getId(), message, oob);
		for (ChatAvatar member : room.getMembers()) {
			Player player = getPlayer(member);
			if (player == null)
				continue;
			PlayerObject playerObject = player.getPlayerObject();
			if (playerObject != null && playerObject.isIgnored(sender.getName()))
				continue;
			
			player.sendPacket(chatRoomMessage);
		}
	}
	
	private static void sendPacketToMembers(ChatRoom room, SWGPacket packet) {
		room.getMembers()
				.stream()
				.map(ChatRoomHandler::getPlayer)
				.filter(Objects::nonNull)	// Don't try sending packets to players that can't be found
				.forEach(player -> player.sendPacket(packet));
	}
	
	@Nullable
	private static Player getPlayer(ChatAvatar avatar) {
		return PlayerLookup.getPlayerByFirstName(avatar.getName());
	}
	
}
