/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package services.chat;

import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import intents.chat.ChatRoomUpdateIntent;
import intents.network.GalacticPacketIntent;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.chat.ChatAddModeratorToRoom;
import network.packets.swg.zone.chat.ChatBanAvatarFromRoom;
import network.packets.swg.zone.chat.ChatCreateRoom;
import network.packets.swg.zone.chat.ChatDestroyRoom;
import network.packets.swg.zone.chat.ChatEnterRoomById;
import network.packets.swg.zone.chat.ChatInviteAvatarToRoom;
import network.packets.swg.zone.chat.ChatKickAvatarFromRoom;
import network.packets.swg.zone.chat.ChatOnAddModeratorToRoom;
import network.packets.swg.zone.chat.ChatOnBanAvatarFromRoom;
import network.packets.swg.zone.chat.ChatOnCreateRoom;
import network.packets.swg.zone.chat.ChatOnDestroyRoom;
import network.packets.swg.zone.chat.ChatOnEnteredRoom;
import network.packets.swg.zone.chat.ChatOnInviteToRoom;
import network.packets.swg.zone.chat.ChatOnKickAvatarFromRoom;
import network.packets.swg.zone.chat.ChatOnLeaveRoom;
import network.packets.swg.zone.chat.ChatOnReceiveRoomInvitation;
import network.packets.swg.zone.chat.ChatOnRemoveModeratorFromRoom;
import network.packets.swg.zone.chat.ChatOnSendRoomMessage;
import network.packets.swg.zone.chat.ChatOnUnbanAvatarFromRoom;
import network.packets.swg.zone.chat.ChatOnUninviteFromRoom;
import network.packets.swg.zone.chat.ChatQueryRoom;
import network.packets.swg.zone.chat.ChatQueryRoomResults;
import network.packets.swg.zone.chat.ChatRemoveAvatarFromRoom;
import network.packets.swg.zone.chat.ChatRemoveModeratorFromRoom;
import network.packets.swg.zone.chat.ChatRequestRoomList;
import network.packets.swg.zone.chat.ChatSendToRoom;
import network.packets.swg.zone.chat.ChatUnbanAvatarFromRoom;
import network.packets.swg.zone.chat.ChatUninviteFromRoom;
import network.packets.swg.zone.insertion.ChatRoomList;
import resources.Terrain;
import resources.chat.ChatAvatar;
import resources.chat.ChatResult;
import resources.chat.ChatRoom;
import resources.client_info.ServerFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.player.PlayerObject;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.Log;
import resources.server_info.ObjectDatabase;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;
import services.CoreManager;
import services.chat.ChatManager.ChatRange;
import services.chat.ChatManager.ChatType;
import services.player.PlayerManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import resources.encodables.OutOfBandPackage;

/**
 * @author Waverunner
 */
public class ChatRoomService extends Service {
	// Map to keep track of each player's recent message for a room to prevent duplicates from client
	private final Map<Long, Map<Integer, Integer>> messages;
	private final ObjectDatabase<ChatRoom> database;
	private final Map<Integer, ChatRoom> roomMap;
	private final RelationalServerData chatLogs;
	private final PreparedStatement insertChatLog;
	private int maxChatRoomId;

	public ChatRoomService() {
		database	= new CachedObjectDatabase<ChatRoom>("odb/chat_rooms.db", ChatRoom::create, (r, s)->r.save(s));
		roomMap 	= new ConcurrentHashMap<>();
		messages	= new ConcurrentHashMap<>();
		chatLogs	= RelationalServerFactory.getServerDatabase("chat/chat_log.db");
		insertChatLog = chatLogs.prepareStatement("INSERT INTO chat_log VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		maxChatRoomId = 1;
		
		registerForIntent(ChatRoomUpdateIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
	}

	@Override
	public boolean initialize() {
		database.load();
		database.traverse((room) -> {
			if (room.getId() >= maxChatRoomId)
				maxChatRoomId++;
			roomMap.put(room.getId(), room);
		});

		createSystemChannels(CoreManager.getGalaxy().getName());
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		database.close();
		return super.terminate();
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case ChatRoomUpdateIntent.TYPE:
				if (i instanceof ChatRoomUpdateIntent)
					processChatRoomUpdateIntent((ChatRoomUpdateIntent) i);
				break;
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					processPacket((GalacticPacketIntent) i);
				break;
			case PlayerEventIntent.TYPE:
				if (i instanceof PlayerEventIntent)
					handlePlayerEventIntent((PlayerEventIntent) i);
				break;
		}
	}

	private void processPacket(GalacticPacketIntent intent) {
		Player player = intent.getPlayerManager().getPlayerFromNetworkId(intent.getNetworkId());
		if (player == null)
			return;

		Packet p = intent.getPacket();
		if (p instanceof SWGPacket)
			processSwgPacket(player, (SWGPacket) p);
	}

	private void processSwgPacket(Player player, SWGPacket p) {
		switch (p.getPacketType()) {
			case CHAT_QUERY_ROOM:
				if (p instanceof ChatQueryRoom) handleChatQueryRoom(player, (ChatQueryRoom) p);
				break;
			case CHAT_ENTER_ROOM_BY_ID: {
				if (!(p instanceof ChatEnterRoomById)) return;
				ChatEnterRoomById enterRoomById = (ChatEnterRoomById) p;
				enterChatChannel(player, enterRoomById.getRoomId(), enterRoomById.getSequence());
				break; }
			case CHAT_REMOVE_AVATAR_FROM_ROOM:
				if (p instanceof ChatRemoveAvatarFromRoom) leaveChatChannel(player, ((ChatRemoveAvatarFromRoom) p).getPath());
				break;
			case CHAT_SEND_TO_ROOM:
				if (p instanceof ChatSendToRoom) handleChatSendToRoom(player, (ChatSendToRoom) p);
				break;
			case CHAT_REQUEST_ROOM_LIST:
				if (p instanceof ChatRequestRoomList) handleChatRoomListRequest(player);
				break;
			case CHAT_CREATE_ROOM:
				if (p instanceof ChatCreateRoom) handleChatCreateRoom(player, (ChatCreateRoom) p);
				break;
			case CHAT_DESTROY_ROOM:
				if (p instanceof ChatDestroyRoom) handleChatDestroyRoom(player, (ChatDestroyRoom) p);
				break;
			case CHAT_INVITE_AVATAR_TO_ROOM:
				if (p instanceof ChatInviteAvatarToRoom) handleChatInviteToRoom(player, (ChatInviteAvatarToRoom) p);
				break;
			case CHAT_UNINVITE_FROM_ROOM:
				if (p instanceof ChatUninviteFromRoom) handleChatUninviteFromRoom(player, (ChatUninviteFromRoom) p);
				break;
			case CHAT_KICK_AVATAR_FROM_ROOM:
				if (p instanceof ChatKickAvatarFromRoom) handleChatKickAvatarFromRoom(player, (ChatKickAvatarFromRoom) p);
				break;
			case CHAT_BAN_AVATAR_FROM_ROOM:
				if (p instanceof ChatBanAvatarFromRoom) handleChatBanAvatarFromRoom(player, (ChatBanAvatarFromRoom) p);
				break;
			case CHAT_UNBAN_AVATAR_FROM_ROOM:
				if (p instanceof ChatUnbanAvatarFromRoom) handleChatUnbanAvatarFromRoom(player, (ChatUnbanAvatarFromRoom) p);
				break;
			case CHAT_ADD_MODERATOR_TO_ROOM:
				if (p instanceof ChatAddModeratorToRoom) handleChatAddModeratorToRoom(player, (ChatAddModeratorToRoom) p);
				break;
			case CHAT_REMOVE_MODERATOR_FROM_ROOM:
				if (p instanceof ChatRemoveModeratorFromRoom) handleChatRemoveModeratorFromRoom(player, (ChatRemoveModeratorFromRoom) p);
				break;
			default: break;
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		Player player = intent.getPlayer();
		if (player == null)
			return;

		switch (intent.getEvent()) {
			case PE_ZONE_IN_CLIENT:
				enterPlanetaryChatChannels(player);
				break;
			case PE_FIRST_ZONE:
				if (player.getPlayerObject() != null)
					enterChatChannels(player, player.getPlayerObject().getJoinedChannels());
				break;
			default:
				break;
		}
	}

	private void processChatRoomUpdateIntent(ChatRoomUpdateIntent i) {
		switch(i.getUpdateType()) {
			case CREATE: createRoom(i.getAvatar(), i.isPublic(), i.getPath(), i.getTitle()); break;
			case DESTROY: notifyDestroyRoom(i.getAvatar(), i.getPath(), 0); break;
			case JOIN: enterChatChannel(i.getPlayer(), i.getPath(), i.isIgnoreInvitation()); break;
			case LEAVE: leaveChatChannel(i.getPlayer(), i.getPath()); break;
			case SEND_MESSAGE: sendMessageToRoom(i.getPlayer(), getRoom(i.getPath()), 0, i.getMessage(), new OutOfBandPackage());  break;
			default: break;
		}
	}

	/* Chat Rooms */

	private void handleChatRemoveModeratorFromRoom(Player player, ChatRemoveModeratorFromRoom p) {
		String path = p.getRoom();
		ChatAvatar target = p.getAvatar();
		int sequence = p.getSequence();

		ChatRoom room = getRoom(path);
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);

		if (room == null) {
			player.sendPacket(new ChatOnRemoveModeratorFromRoom(target, sender, ChatResult.ROOM_INVALID_NAME.getCode(), path, sequence));
			return;
		}

		if (!room.isModerated()) {
			player.sendPacket(new ChatOnRemoveModeratorFromRoom(target, sender, ChatResult.CUSTOM_FAILURE.getCode(), path, sequence));
			return;
		}

		if (!room.isModerator(sender)) {
			player.sendPacket(new ChatOnRemoveModeratorFromRoom(target, sender, ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode(), path, sequence));
			return;
		}

		if (!room.removeModerator(target)) {
			player.sendPacket(new ChatOnRemoveModeratorFromRoom(target, sender, ChatResult.TARGET_AVATAR_DOESNT_EXIST.getCode(), path, sequence));
			return;
		}

		player.sendPacket(new ChatOnRemoveModeratorFromRoom(target, sender, ChatResult.SUCCESS.getCode(), path, sequence));
	}

	private void handleChatAddModeratorToRoom(Player player, ChatAddModeratorToRoom p) {
		String path = p.getRoom();
		ChatAvatar target = p.getAvatar();
		int sequence = p.getSequence();

		ChatRoom room = getRoom(path);
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);

		if (room == null) {
			player.sendPacket(new ChatOnAddModeratorToRoom(target, sender, ChatResult.ROOM_INVALID_NAME.getCode(), path, sequence));
			return;
		}

		if (!room.isModerated()) {
			player.sendPacket(new ChatOnAddModeratorToRoom(target, sender, ChatResult.CUSTOM_FAILURE.getCode(), path, sequence));
			return;
		}

		if (!room.isModerator(sender)) {
			player.sendPacket(new ChatOnAddModeratorToRoom(target, sender, ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode(), path, sequence));
			return;
		}

		Player targetPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(target.getName());
		if (targetPlayer == null) {
			player.sendPacket(new ChatOnAddModeratorToRoom(target, sender, ChatResult.TARGET_AVATAR_DOESNT_EXIST.getCode(), path, sequence));
			return;
		}

		if (room.removeBanned(target)) {
			// Remove player from the ban list for players that have joined the room, since this player is now a moderator
			room.sendPacketToMembers(player.getPlayerManager(), new ChatOnUnbanAvatarFromRoom(path, sender, target, ChatResult.SUCCESS.getCode(), 0));
		}

		if (!room.addModerator(target)) {
			player.sendPacket(new ChatOnAddModeratorToRoom(target, sender, ChatResult.NONE.getCode(), path, sequence));
			return;
		}

		player.sendPacket(new ChatOnAddModeratorToRoom(target, sender, ChatResult.SUCCESS.getCode(), path, sequence));
	}

	private void handleChatUnbanAvatarFromRoom(Player player, ChatUnbanAvatarFromRoom p) {
		String path = p.getRoom();
		ChatAvatar target = p.getAvatar();
		int sequence = p.getSequence();

		ChatRoom room = getRoom(path);
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);

		if (room == null) {
			player.sendPacket(new ChatOnUnbanAvatarFromRoom(path, sender, target, ChatResult.ROOM_INVALID_NAME.getCode(), sequence));
			return;
		}

		if (!room.isModerator(sender)) {
			player.sendPacket(new ChatOnUnbanAvatarFromRoom(path, sender, target, ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode(), sequence));
			return;
		}

		if (!room.isBanned(target) || !room.removeBanned(target)) {
			player.sendPacket(new ChatOnUnbanAvatarFromRoom(path, sender, target, ChatResult.ROOM_AVATAR_BANNED.getCode(), sequence));
			return;
		}

		room.sendPacketToMembers(player.getPlayerManager(), new ChatOnUnbanAvatarFromRoom(path, sender, target, ChatResult.SUCCESS.getCode(), sequence));
	}

	private void handleChatBanAvatarFromRoom(Player player, ChatBanAvatarFromRoom p) {
		String path = p.getRoom();
		ChatAvatar target = p.getAvatar();
		int sequence = p.getSequence();

		ChatRoom room = getRoom(path);
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);

		if (room == null) {
			player.sendPacket(new ChatOnBanAvatarFromRoom(path, sender, target, ChatResult.ROOM_INVALID_NAME.getCode(), sequence));
			return;
		}

		if (!room.isModerator(sender)) {
			player.sendPacket(new ChatOnBanAvatarFromRoom(path, sender, target, ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode(), sequence));
			return;
		}

		if (room.isBanned(target)) {
			player.sendPacket(new ChatOnBanAvatarFromRoom(path, sender, target, ChatResult.ROOM_AVATAR_BANNED.getCode(), sequence));
			return;
		}

		if (!room.isMember(target)) {
			player.sendPacket(new ChatOnBanAvatarFromRoom(path, sender, target, ChatResult.TARGET_AVATAR_DOESNT_EXIST.getCode(), sequence));
			return;
		}

		if (room.isModerator(target))
			room.removeModerator(target);

		if (room.isInvited(target))
			room.removeInvited(target);

		room.addBanned(target);

		room.sendPacketToMembers(player.getPlayerManager(), new ChatOnBanAvatarFromRoom(path, sender, target, ChatResult.SUCCESS.getCode(), sequence));
	}

	private void handleChatKickAvatarFromRoom(Player player, ChatKickAvatarFromRoom p) {
		String path = p.getRoom();
		ChatAvatar target = p.getAvatar();

		ChatRoom room = getRoom(path);
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);

		if (room == null) {
			player.sendPacket(new ChatOnKickAvatarFromRoom(target, sender, ChatResult.ROOM_INVALID_NAME.getCode(), path));
			return;
		}

		if (!room.isModerator(sender)) {
			player.sendPacket(new ChatOnKickAvatarFromRoom(target, sender, ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode(), path));
			return;
		}

		if (!room.isMember(target)) {
			player.sendPacket(new ChatOnKickAvatarFromRoom(target, sender, ChatResult.TARGET_AVATAR_DOESNT_EXIST.getCode(), path));
			return;
		}

		Player targetPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(target.getName());
		if (targetPlayer == null) {
			player.sendPacket(new ChatOnKickAvatarFromRoom(target, sender, ChatResult.TARGET_AVATAR_DOESNT_EXIST.getCode(), path));
			return;
		}

		room.sendPacketToMembers(player.getPlayerManager(), new ChatOnKickAvatarFromRoom(target, sender, ChatResult.SUCCESS.getCode(), path));
	}

	private void handleChatUninviteFromRoom(Player player, ChatUninviteFromRoom p) {
		String path = p.getRoom();
		ChatRoom room = getRoom(path);

		ChatAvatar invitee = p.getAvatar();
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);

		if (room == null) {
			player.sendPacket(new ChatOnUninviteFromRoom(path, sender, invitee, ChatResult.ROOM_INVALID_NAME.getCode(), p.getSequence()));
			return;
		}

		if (room.isPublic()) {
			player.sendPacket(new ChatOnUninviteFromRoom(path, sender, invitee, ChatResult.CUSTOM_FAILURE.getCode(), p.getSequence()));
			return;
		}

		if (!room.isModerator(sender)) {
			player.sendPacket(new ChatOnUninviteFromRoom(path, sender, invitee, ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode(), p.getSequence()));
			return;
		}

		if (!room.removeInvited(invitee)) {
			player.sendPacket(new ChatOnUninviteFromRoom(path, sender, invitee, ChatResult.ROOM_PRIVATE.getCode(), p.getSequence()));
			return;
		}

		player.sendPacket(new ChatOnUninviteFromRoom(path, sender, invitee, ChatResult.SUCCESS.getCode(), p.getSequence()));
	}

	private void handleChatInviteToRoom(Player player, ChatInviteAvatarToRoom p) {
		String path = p.getRoom();
		ChatRoom room = getRoom(path);

		ChatAvatar sender = ChatAvatar.getFromPlayer(player);
		if (room == null) {
			player.sendPacket(new ChatOnInviteToRoom(path, sender, p.getAvatar(), ChatResult.ROOM_INVALID_NAME.getCode()));
			return;
		}

		if (room.isPublic()) {
			player.sendPacket(new ChatOnInviteToRoom(path, sender, p.getAvatar(), ChatResult.CUSTOM_FAILURE.getCode()));
			return;
		}

		if (!room.isModerator(sender)) {
			player.sendPacket(new ChatOnInviteToRoom(path, sender, p.getAvatar(), ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode()));
			return;
		}

		ChatAvatar invitee = p.getAvatar();
		Player invitedPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(invitee.getName());
		if (invitedPlayer == null) {
			player.sendPacket(new ChatOnInviteToRoom(path, sender, invitee, ChatResult.TARGET_AVATAR_DOESNT_EXIST.getCode()));
			return;
		}

		player.sendPacket(new ChatOnInviteToRoom(path, sender, invitee, ChatResult.SUCCESS.getCode()));

		room.addInvited(invitee);

		// Notify the invited client that the room exists if not already in the clients chat lists
		invitedPlayer.sendPacket(new ChatRoomList(room));

		invitedPlayer.sendPacket(new ChatOnReceiveRoomInvitation(sender, path));
	}

	private void handleChatDestroyRoom(Player player, ChatDestroyRoom p) {
		ChatRoom room = roomMap.get(p.getRoomId());
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		if ((room == null || !room.getCreator().equals(avatar) || !room.getOwner().equals(avatar))) {
			player.sendPacket(new ChatOnDestroyRoom(avatar, ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode(), p.getRoomId(), p.getSequence()));
			return;
		}

		if (!notifyDestroyRoom(avatar, room.getPath(), p.getSequence())) {
			player.sendPacket(new ChatOnDestroyRoom(avatar, ChatResult.NONE.getCode(), p.getRoomId(), p.getSequence()));
			return;
		}

		player.sendPacket(new ChatOnDestroyRoom(avatar, ChatResult.SUCCESS.getCode(), p.getRoomId(), p.getSequence()));
	}

	private void handleChatCreateRoom(Player player, ChatCreateRoom p) {
		String path = p.getRoomName();
		String title = p.getRoomTitle();
		ChatRoom room = getRoom(path);

		ChatResult result = ChatResult.SUCCESS;
		if (room != null)
			result = ChatResult.ROOM_ALREADY_EXISTS;

		if (result == ChatResult.SUCCESS) {
			room = createRoom(ChatAvatar.getFromPlayer(player), p.isPublic(), path, title, true);
			room.setModerated(p.isModerated());
		}

		player.sendPacket(new ChatOnCreateRoom(result.getCode(), room, p.getSequence()));
	}

	private void handleChatSendToRoom(Player player, ChatSendToRoom p) {
		ChatRoom room = getRoom(p.getRoomId());
		sendMessageToRoom(player, room, p.getSequence(), p.getMessage(), p.getOutOfBandPackage());
	}
	
	private void sendMessageToRoom(Player player, ChatRoom room, int sequence, String message, OutOfBandPackage outOfBandPackage) {
		ChatResult result = ChatResult.SUCCESS;
		
		if (room == null)
			result = ChatResult.ROOM_INVALID_ID;
		System.out.println("Room result: " + result);
		System.out.println("player: " + player);
		if (result != ChatResult.SUCCESS) {
			player.sendPacket(new ChatOnSendRoomMessage(result.getCode(), sequence));
			return;
		}

		if (!incrementMessageCounter(player.getNetworkId(), room.getId(), sequence))
			return;

		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		result = room.canSendMessage(avatar);

		// TODO: Check length of messages -- Result 16 is used for too long message

		player.sendPacket(new ChatOnSendRoomMessage(result.getCode(), sequence));

		if (result == ChatResult.SUCCESS) {
			room.sendMessage(avatar, message, outOfBandPackage, player.getPlayerManager());
			logChat(player.getCreatureObject().getObjectId(), player.getCharacterName(), room.getId()+"/"+room.getPath(), message);
		}
	}

	private void handleChatQueryRoom(Player player, ChatQueryRoom p) {
		ChatRoom room = getRoom(p.getRoomPath()); // No result code is sent for queries
		if (room == null)
			return;

		player.sendPacket(new ChatQueryRoomResults(room, p.getSequence()));
	}

	private void handleChatRoomListRequest(Player player) {
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		List<ChatRoom> rooms = new ArrayList<>();
		for (ChatRoom chatRoom : roomMap.values()) {
			if (!chatRoom.isPublic() && !chatRoom.isInvited(avatar) && !chatRoom.getOwner().equals(avatar))
				continue;
			rooms.add(chatRoom);
		}

		ChatRoomList response = new ChatRoomList(rooms);
		player.sendPacket(response);
	}

	private void sendOnEnteredChatRoom(Player player, ChatAvatar avatar, ChatResult result, int id, int sequence) {
		ChatOnEnteredRoom onEnteredRoom = new ChatOnEnteredRoom(avatar, id, sequence);
		onEnteredRoom.setResult(result.getCode());
		player.sendPacket(onEnteredRoom);
	}

	public void enterChatChannels(Player player, List<String> channels) {
		for (String s : channels) {
			enterChatChannel(player, s, false);
		}
	}

	public void enterPlanetaryChatChannels(Player player) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		// Leave old zone-only chat channels
		String planetEndPath = ".Planet";
		for (String channel : ghost.getJoinedChannels()) {
			if (channel.endsWith(planetEndPath)) {
				leaveChatChannel(player, channel);
			} else {
				// Better way of doing this?
				String[] split = channel.split("\\.");
				if (split.length == 3 && split[2].equals("system"))
					leaveChatChannel(player, channel);
			}
		}

		Terrain terrain = player.getCreatureObject().getTerrain();

		// Enter the new zone-only chat channels
		String planetPath = "SWG." + player.getGalaxyName() + "." + terrain.getName() + ".";
		if (getRoom(planetPath + "Planet") == null)
			return;

		enterChatChannel(player, planetPath + "Planet", false);
		enterChatChannel(player, planetPath + "system", false);
	}

	/**
	 * Attempts to join the specified chat channel
	 * @param player Player joining the chat channel
	 * @param room Chat room to enter
	 */
	public void enterChatChannel(Player player, ChatRoom room, int sequence, boolean ignoreInvitation) {
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null) {
			sendOnEnteredChatRoom(player, avatar, ChatResult.NONE, room.getId(), sequence);
			return;
		}

		ChatResult result = room.canJoinRoom(avatar, ignoreInvitation);
		System.out.println(player + " canJoinRoom: " + result);
		if (result != ChatResult.SUCCESS && player.getAccessLevel() != AccessLevel.PLAYER) {
			sendOnEnteredChatRoom(player, avatar, result, room.getId(), sequence);
			return;
		}
		// TODO: Check if player is appropriate faction for the room (Rebel and imperial chat rooms)

		// Server-based list so we can join chat channels automatically
		ghost.addJoinedChannel(room.getPath());

		// Re-send the player the room list with just this room as it could have been private/hidden
		// This also "refreshes" the client, not sending this will cause a Chat channel unavailable message.
		if (!room.isPublic())
			player.sendPacket(new ChatRoomList(room));

		// Notify players of success, it's ChatResult.SUCCESS at this point
		player.sendPacket(new ChatOnEnteredRoom(avatar, result.getCode(), room.getId(), sequence));

		PlayerManager manager = player.getPlayerManager();
		// Notify everyone that a player entered the room
		room.sendPacketToMembers(manager, new ChatOnEnteredRoom(avatar, result.getCode(), room.getId(), 0));

		room.addMember(avatar);
	}

	public void enterChatChannel(Player player, int id, int sequence) {
		ChatRoom room = getRoom(id);
		if (room == null) {
			sendOnEnteredChatRoom(player, ChatAvatar.getFromPlayer(player), ChatResult.NONE, id, sequence);
			return;
		}
		enterChatChannel(player, room, sequence, false);
	}

	public void enterChatChannel(Player player, String path, boolean ignoreInvitation) {
		for (ChatRoom room : roomMap.values()) {
			if (room.getPath().equals(path)) {
				enterChatChannel(player, room, 0, ignoreInvitation);
				return;
			}
		}
		// Channel was not found, attempt to remove it from this players list of channels if it exists.
		// This can happen if a channel was deleted while the player was offline
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null) {
			Log.e(this, "Tried to join a room with a path that does not exist: " + path);
			return;
		}
		ghost.removeJoinedChannel(path);
	}

	public void leaveChatChannel(Player player, ChatRoom room, int sequence) {
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return; // ChatOnLeaveRoom doesn't do anything other than for a ChatResult.SUCCESS, so no need to send a fail

		if (!room.removeMember(avatar) && !ghost.removeJoinedChannel(room.getPath()))
			return;

		player.sendPacket(new ChatOnLeaveRoom(avatar, ChatResult.SUCCESS.getCode(), room.getId(), sequence));

		room.sendPacketToMembers(player.getPlayerManager(), new ChatOnLeaveRoom(avatar, ChatResult.SUCCESS.getCode(), room.getId(), 0));
	}

	public void leaveChatChannel(Player player, String path) {
		for (ChatRoom chatRoom : roomMap.values()) {
			if (chatRoom.getPath().equals(path)) {
				leaveChatChannel(player, chatRoom, 0);
				break;
			}
		}
	}

	/**
	 * Creates a new chat room with the specified address path. If the path's parent channel doesn't exist, then a new
	 * chat room is created with the same passed arguments.
	 * @param creator Room creator who will also become the owner of this room
	 * @param isPublic Determines if the room should be publicly displayed in the channel listing
	 * @param path Address for the channel (Ex: SWG.serverName.Imperial)
	 * @param title Descriptive name of the chat channel (Ex: Imperial chat for this galaxy)
	 * @param persist If true then this channel will be saved in an {@link ObjectDatabase}
	 * @return {@link ChatRoom}
	 */
	public ChatRoom createRoom(ChatAvatar creator, boolean isPublic, String path, String title, boolean persist) {
		if (path.isEmpty() || path.endsWith("."))
			return null;

		String base = "SWG." + creator.getGalaxy();
		if (!path.startsWith(base) || path.equals(base))
			return null;

		if (getRoom(path) != null)
			return getRoom(path);

		// All paths should have parents, lets validate to make sure they exist first. Create them if they don't.
		int lastIndex = path.lastIndexOf('.');
		if (lastIndex != -1) {
			String parentPath = path.substring(0, lastIndex);
			if (getRoom(parentPath) == null) {
				createRoom(creator, isPublic, parentPath, "", persist);
			}
		}

		ChatRoom room = new ChatRoom();
		int id = maxChatRoomId;
		maxChatRoomId++;

		room.setId(id);
		room.setOwner(creator);
		room.setCreator(creator);
		room.setIsPublic(isPublic);
		room.setPath(path);
		room.setTitle(title);
		room.addModerator(creator);

		roomMap.put(id, room);

		if (persist)
			database.add(room);
		return room;
	}

	/**
	 * Creates a new, non-persistent, chat room with the specified address path.
	 * @param creator Room creator who will also become the owner of this room
	 * @param isPublic Determines if the room should be publicly displayed in the channel listing
	 * @param path Address for the channel (Ex: SWG.serverName.Imperial)
	 * @param title Descriptive name of the chat channel (Ex: Imperial chat for this galaxy)
	 * @return {@link ChatRoom}
	 */
	public ChatRoom createRoom(ChatAvatar creator, boolean isPublic, String path, String title) {
		return createRoom(creator, isPublic, path, title, false);
	}

	public boolean notifyDestroyRoom(ChatAvatar destroyer, String roomPath, int sequence) {
		ChatRoom room = getRoom(roomPath);
		if (roomPath == null)
			return false;

		if (!destroyRoom(room))
			return false;

		// Send the ChatOnDestroyRoom packet to every else in the room besides the person destroying the packet
		List<Long> networkIds = new ArrayList<>();
		room.getMembers().forEach(member -> {
			if (!destroyer.equals(member))
				networkIds.add(member.getNetworkId());
		});

		new NotifyPlayersPacketIntent(new ChatOnDestroyRoom(destroyer, ChatResult.SUCCESS.getCode(), room.getId(), 0),
				networkIds).broadcast();

		return true;
	}

	public boolean destroyRoom(ChatRoom room) {
		return room != null && roomMap.remove(room.getId()) != null;
	}

	public void createSystemChannels(String galaxy) {
		/** Channel Notes
		 * Group channels: created by System
		 * 	- SWG.serverName.group.GroupObjectId.GroupChat 	title = GroupId
		 * Guild channels: created by System
		 * 	- SWG.serverName.guild.GuildId.GuildChat 		title = GuildId
		 * City channels: created by System
		 * 	- SWG.serverName.city.CityId.CityChat			title = CityId
		 */

		ChatAvatar systemAvatar = ChatAvatar.getSystemAvatar(galaxy);
		String basePath = "SWG." + galaxy + ".";

		DatatableData rooms = ServerFactory.getDatatable("chat/default_rooms.iff");
		rooms.handleRows((r) -> createRoom(systemAvatar, true, basePath + rooms.getCell(r, 0), (String) rooms.getCell(r, 1), true));

		createPlanetChannels(systemAvatar, basePath);

		/*
		Battlefield Room path examples:
		SWG.Bria.corellia.battlefield
		SWG.Bria.corellia.battlefield.corellia_mountain_fortress.allFactions
		SWG.Bria.corellia.battlefield.corellia_pvp.allFactions / Imperial / Rebel
		SWG.Bria.corellia.battlefield.corellia_rebel_riverside_fort.allFactions
		 */
	}

	private void createPlanetChannels(ChatAvatar systemAvatar, String basePath) {
		DatatableData planets = ServerFactory.getDatatable("chat/planets.iff");
		planets.handleRows((r) -> {
			String path = basePath + planets.getCell(r, 0) + ".";
			createRoom(systemAvatar, true, path + "Planet", "public chat for this planet, cannot create rooms here", true);
			createRoom(systemAvatar, true, path + "system", "system messages for this planet, cannot create rooms here", true);
			createRoom(systemAvatar, true, path + "Chat", "public chat for this planet, can create rooms here", true);
		});
	}

	private boolean incrementMessageCounter(long networkId, int roomId, int messageId) {
		if (networkId == -1) // System ChatAvatar uses -1 for networkId
			return true;

		Map<Integer, Integer> messageHistory = messages.get(networkId);
		if (messageHistory != null) {
			if (messageHistory.containsKey(roomId) && messageHistory.get(roomId) == messageId)
				return false;
		} else {
			messageHistory = new HashMap<>();
			messages.put(networkId, messageHistory);
		}
		messageHistory.put(roomId, messageId);
		return true;
	}

	public ChatRoom getRoom(String path) {
		for (ChatRoom room : roomMap.values()) {
			if (room.getPath().equals(path))
				return room;
		}
		return null;
	}

	public ChatRoom getRoom(int roomId) {
		return roomMap.get(roomId);
	}
	
	private void logChat(long sendId, String sendName, String room, String message) {
		try {
			synchronized (insertChatLog) {
				insertChatLog.setLong(1, System.currentTimeMillis());
				insertChatLog.setLong(2, sendId);
				insertChatLog.setString(3, sendName);
				insertChatLog.setLong(4, 0);
				insertChatLog.setString(5, "");
				insertChatLog.setString(6, ChatType.CHAT.name());
				insertChatLog.setString(7, ChatRange.ROOM.name());
				insertChatLog.setString(8, room);
				insertChatLog.setString(9, "");
				insertChatLog.setString(10, message);
				insertChatLog.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
