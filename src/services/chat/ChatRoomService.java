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
import network.packets.swg.zone.chat.ChatOnInviteToRoom;
import network.packets.swg.zone.chat.ChatOnKickAvatarFromRoom;
import network.packets.swg.zone.chat.ChatOnReceiveRoomInvitation;
import network.packets.swg.zone.chat.ChatOnRemoveModeratorFromRoom;
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
import resources.chat.ChatAvatar;
import resources.chat.ChatResult;
import resources.chat.ChatRoom;
import resources.encodables.OutOfBandPackage;
import resources.player.Player;

import com.projectswg.common.control.Service;

public class ChatRoomService extends Service {
	
	private final ChatRoomHandler chatRoomHandler;
	
	public ChatRoomService() {
		chatRoomHandler = new ChatRoomHandler();
		
		registerForIntent(ChatRoomUpdateIntent.class, crui -> handleChatRoomUpdateIntent(crui));
		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
	}
	
	@Override
	public boolean initialize() {
		return super.initialize() && chatRoomHandler.initialize();
	}
	
	@Override
	public boolean terminate() {
		return chatRoomHandler.terminate() && super.terminate();
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet p = gpi.getPacket();
		if (p instanceof SWGPacket)
			processSwgPacket(gpi.getPlayer(), (SWGPacket) p);
	}
	
	private void processSwgPacket(Player player, SWGPacket p) {
		switch (p.getPacketType()) {
			case CHAT_QUERY_ROOM:
				if (p instanceof ChatQueryRoom)
					handleChatQueryRoom(player, (ChatQueryRoom) p);
				break;
			case CHAT_ENTER_ROOM_BY_ID:
				if (p instanceof ChatEnterRoomById)
					chatRoomHandler.enterChatChannel(player, ((ChatEnterRoomById) p).getRoomId(), ((ChatEnterRoomById) p).getSequence());
				break;
			case CHAT_REMOVE_AVATAR_FROM_ROOM:
				if (p instanceof ChatRemoveAvatarFromRoom)
					chatRoomHandler.leaveChatChannel(player, ((ChatRemoveAvatarFromRoom) p).getPath());
				break;
			case CHAT_SEND_TO_ROOM:
				if (p instanceof ChatSendToRoom)
					handleChatSendToRoom(player, (ChatSendToRoom) p);
				break;
			case CHAT_REQUEST_ROOM_LIST:
				if (p instanceof ChatRequestRoomList)
					handleChatRoomListRequest(player);
				break;
			case CHAT_CREATE_ROOM:
				if (p instanceof ChatCreateRoom)
					handleChatCreateRoom(player, (ChatCreateRoom) p);
				break;
			case CHAT_DESTROY_ROOM:
				if (p instanceof ChatDestroyRoom)
					handleChatDestroyRoom(player, (ChatDestroyRoom) p);
				break;
			case CHAT_INVITE_AVATAR_TO_ROOM:
				if (p instanceof ChatInviteAvatarToRoom)
					handleChatInviteToRoom(player, (ChatInviteAvatarToRoom) p);
				break;
			case CHAT_UNINVITE_FROM_ROOM:
				if (p instanceof ChatUninviteFromRoom)
					handleChatUninviteFromRoom(player, (ChatUninviteFromRoom) p);
				break;
			case CHAT_KICK_AVATAR_FROM_ROOM:
				if (p instanceof ChatKickAvatarFromRoom)
					handleChatKickAvatarFromRoom(player, (ChatKickAvatarFromRoom) p);
				break;
			case CHAT_BAN_AVATAR_FROM_ROOM:
				if (p instanceof ChatBanAvatarFromRoom)
					handleChatBanAvatarFromRoom(player, (ChatBanAvatarFromRoom) p);
				break;
			case CHAT_UNBAN_AVATAR_FROM_ROOM:
				if (p instanceof ChatUnbanAvatarFromRoom)
					handleChatUnbanAvatarFromRoom(player, (ChatUnbanAvatarFromRoom) p);
				break;
			case CHAT_ADD_MODERATOR_TO_ROOM:
				if (p instanceof ChatAddModeratorToRoom)
					handleChatAddModeratorToRoom(player, (ChatAddModeratorToRoom) p);
				break;
			case CHAT_REMOVE_MODERATOR_FROM_ROOM:
				if (p instanceof ChatRemoveModeratorFromRoom)
					handleChatRemoveModeratorFromRoom(player, (ChatRemoveModeratorFromRoom) p);
				break;
			default:
				break;
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_ZONE_IN_CLIENT:
				chatRoomHandler.enterChatChannels(pei.getPlayer());
				chatRoomHandler.enterPlanetaryChatChannels(pei.getPlayer());
				break;
			default:
				break;
		}
	}
	
	private void handleChatRoomUpdateIntent(ChatRoomUpdateIntent crui) {
		switch (crui.getUpdateType()) {
			case CREATE:
				chatRoomHandler.createRoom(crui.getAvatar(), crui.isPublic(), false, crui.getPath(), crui.getTitle(), false);
				break;
			case DESTROY:
				chatRoomHandler.notifyDestroyRoom(crui.getAvatar(), crui.getPath(), 0);
				break;
			case JOIN:
				chatRoomHandler.enterChatChannel(crui.getPlayer(), crui.getPath(), crui.isIgnoreInvitation());
				break;
			case LEAVE:
				chatRoomHandler.leaveChatChannel(crui.getPlayer(), crui.getPath());
				break;
			case SEND_MESSAGE:
				chatRoomHandler.sendMessageToRoom(crui.getPlayer(), crui.getPath(), 0, crui.getMessage(), new OutOfBandPackage());
				break;
			default:
				break;
		}
	}
	
	/* Chat Rooms */
	
	private void handleChatRemoveModeratorFromRoom(Player player, ChatRemoveModeratorFromRoom p) {
		ChatAvatar target = p.getAvatar();
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);
		ChatResult result = ChatResult.SUCCESS;
		
		if (room == null)
			result = ChatResult.ROOM_INVALID_NAME;
		else if (!room.isModerated())
			result = ChatResult.CUSTOM_FAILURE;
		else if (!room.isModerator(sender))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		else if (!room.removeModerator(target))
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		
		player.sendPacket(new ChatOnRemoveModeratorFromRoom(target, sender, result.getCode(), p.getRoom(), p.getSequence()));
	}
	
	private void handleChatAddModeratorToRoom(Player player, ChatAddModeratorToRoom p) {
		ChatAvatar target = p.getAvatar();
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);
		ChatResult result = ChatResult.SUCCESS;
		
		if (room == null)
			result = ChatResult.ROOM_INVALID_NAME;
		else if (!room.isModerated())
			result = ChatResult.CUSTOM_FAILURE;
		else if (!room.isModerator(sender))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		else if (!room.removeModerator(target) || player.getPlayerManager().getPlayerByCreatureFirstName(target.getName()) == null)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		else if (room.addModerator(target))
			result = ChatResult.NONE;
		
		if (room.removeBanned(target)) {
			// Remove player from the ban list for players that have joined the room, since this player is now a moderator
			room.sendPacketToMembers(player.getPlayerManager(), new ChatOnUnbanAvatarFromRoom(p.getRoom(), sender, target, ChatResult.SUCCESS.getCode(), 0));
		}
		
		player.sendPacket(new ChatOnAddModeratorToRoom(target, sender, result.getCode(), p.getRoom(), p.getSequence()));
	}
	
	private void handleChatUnbanAvatarFromRoom(Player player, ChatUnbanAvatarFromRoom p) {
		ChatAvatar target = p.getAvatar();
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);
		ChatResult result = ChatResult.SUCCESS;
		
		if (room == null)
			result = ChatResult.ROOM_INVALID_NAME;
		else if (!room.isModerator(sender))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		else if (!room.isBanned(target) || !room.removeBanned(target))
			result = ChatResult.ROOM_AVATAR_BANNED;
		
		room.sendPacketToMembers(player.getPlayerManager(), new ChatOnUnbanAvatarFromRoom(p.getRoom(), sender, target, result.getCode(), p.getSequence()));
	}
	
	private void handleChatBanAvatarFromRoom(Player player, ChatBanAvatarFromRoom p) {
		ChatAvatar target = p.getAvatar();
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);
		ChatResult result = ChatResult.SUCCESS;
		
		if (room == null)
			result = ChatResult.ROOM_INVALID_NAME;
		else if (!room.isModerator(sender))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		else if (room.isBanned(target))
			result = ChatResult.ROOM_AVATAR_BANNED;
		else if (!room.isMember(target))
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		
		room.removeModerator(target);
		room.removeInvited(target);
		room.addBanned(target);
		
		room.sendPacketToMembers(player.getPlayerManager(), new ChatOnBanAvatarFromRoom(p.getRoom(), sender, target, result.getCode(), p.getSequence()));
	}
	
	private void handleChatKickAvatarFromRoom(Player player, ChatKickAvatarFromRoom p) {
		ChatAvatar target = p.getAvatar();
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);
		ChatResult result = ChatResult.SUCCESS;
		
		if (room == null)
			result = ChatResult.ROOM_INVALID_NAME;
		else if (!room.isModerator(sender))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		else if (!room.isMember(target))
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		else if (player.getPlayerManager().getPlayerByCreatureFirstName(target.getName()) == null)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		
		room.sendPacketToMembers(player.getPlayerManager(), new ChatOnKickAvatarFromRoom(target, sender, result.getCode(), p.getRoom()));
	}
	
	private void handleChatUninviteFromRoom(Player player, ChatUninviteFromRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar invitee = p.getAvatar();
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);
		ChatResult result = ChatResult.SUCCESS;
		
		if (room == null)
			result = ChatResult.ROOM_INVALID_NAME;
		else if (room.isPublic())
			result = ChatResult.CUSTOM_FAILURE;
		else if (!room.isModerator(sender))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		else if (!room.removeInvited(invitee))
			result = ChatResult.ROOM_PRIVATE;
		
		player.sendPacket(new ChatOnUninviteFromRoom(p.getRoom(), sender, invitee, result.getCode(), p.getSequence()));
	}
	
	private void handleChatInviteToRoom(Player player, ChatInviteAvatarToRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar invitee = p.getAvatar();
		ChatAvatar sender = ChatAvatar.getFromPlayer(player);
		ChatResult result = ChatResult.SUCCESS;
		
		if (room == null)
			result = ChatResult.ROOM_INVALID_NAME;
		else if (room.isPublic())
			result = ChatResult.CUSTOM_FAILURE;
		else if (!room.isModerator(sender))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		
		Player invitedPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(invitee.getName());
		if (result == ChatResult.SUCCESS && invitedPlayer == null)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		
		player.sendPacket(new ChatOnInviteToRoom(p.getRoom(), sender, invitee, result.getCode()));
		
		if (result == ChatResult.SUCCESS) {
			room.addInvited(invitee);
			// Notify the invited client that the room exists if not already in the clients chat lists
			invitedPlayer.sendPacket(new ChatRoomList(room));
			invitedPlayer.sendPacket(new ChatOnReceiveRoomInvitation(sender, p.getRoom()));
		}
	}
	
	private void handleChatDestroyRoom(Player player, ChatDestroyRoom p) {
		ChatRoom room = chatRoomHandler.getRoomById(p.getRoomId());
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);
		ChatResult result = ChatResult.SUCCESS;
		
		if ((room == null || !room.getCreator().equals(avatar) || !room.getOwner().equals(avatar)))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		else if (!chatRoomHandler.notifyDestroyRoom(avatar, room.getPath(), p.getSequence()))
			result = ChatResult.NONE;
		
		player.sendPacket(new ChatOnDestroyRoom(avatar, result.getCode(), p.getRoomId(), p.getSequence()));
	}
	
	private void handleChatCreateRoom(Player player, ChatCreateRoom p) {
		String path = p.getRoomName();
		ChatResult result = ChatResult.SUCCESS;
		
		if (chatRoomHandler.getRoomByPath(path) != null)
			result = ChatResult.ROOM_ALREADY_EXISTS;
		else
			chatRoomHandler.createRoom(ChatAvatar.getFromPlayer(player), p.isPublic(), p.isModerated(), path, p.getRoomTitle(), true);
		
		player.sendPacket(new ChatOnCreateRoom(result.getCode(), chatRoomHandler.getRoomByPath(path), p.getSequence()));
	}
	
	private void handleChatSendToRoom(Player player, ChatSendToRoom p) {
		chatRoomHandler.sendMessageToRoom(player, p.getRoomId(), p.getSequence(), p.getMessage(), p.getOutOfBandPackage());
	}
	
	private void handleChatQueryRoom(Player player, ChatQueryRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoomPath()); // No result code is sent for queries
		if (room == null)
			return;
		
		player.sendPacket(new ChatQueryRoomResults(room, p.getSequence()));
	}
	
	private void handleChatRoomListRequest(Player player) {
		player.sendPacket(new ChatRoomList(chatRoomHandler.getRoomList(player)));
	}
	
}
