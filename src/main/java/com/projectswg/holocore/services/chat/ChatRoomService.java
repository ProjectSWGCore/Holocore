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

package com.projectswg.holocore.services.chat;

import com.projectswg.common.data.encodables.chat.ChatAvatar;
import com.projectswg.common.data.encodables.chat.ChatResult;
import com.projectswg.common.data.encodables.chat.ChatRoom;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.chat.*;
import com.projectswg.common.network.packets.swg.zone.insertion.ChatRoomList;
import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.chat.ChatRoomUpdateIntent;
import com.projectswg.holocore.intents.network.GalacticPacketIntent;
import com.projectswg.holocore.resources.player.AccessLevel;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.services.player.PlayerManager.PlayerLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;

public class ChatRoomService extends Service {
	
	private final ChatRoomHandler chatRoomHandler;
	
	public ChatRoomService() {
		this.chatRoomHandler = new ChatRoomHandler();
	}
	
	@Override
	public boolean initialize() {
		return super.initialize() && chatRoomHandler.initialize();
	}
	
	@Override
	public boolean terminate() {
		return chatRoomHandler.terminate() && super.terminate();
	}
	
	@IntentHandler
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		Player player = gpi.getPlayer();
		switch (packet.getPacketType()) {
			case CHAT_QUERY_ROOM:
				handleChatQueryRoom(player, (ChatQueryRoom) packet);
				break;
			case CHAT_ENTER_ROOM_BY_ID:
				chatRoomHandler.enterChatChannel(player, ((ChatEnterRoomById) packet).getRoomId(), ((ChatEnterRoomById) packet).getSequence());
				break;
			case CHAT_REMOVE_AVATAR_FROM_ROOM:
				chatRoomHandler.leaveChatChannel(player, ((ChatRemoveAvatarFromRoom) packet).getPath());
				break;
			case CHAT_SEND_TO_ROOM:
				handleChatSendToRoom(player, (ChatSendToRoom) packet);
				break;
			case CHAT_REQUEST_ROOM_LIST:
				handleChatRoomListRequest(player);
				break;
			case CHAT_CREATE_ROOM:
				handleChatCreateRoom(player, (ChatCreateRoom) packet);
				break;
			case CHAT_DESTROY_ROOM:
				handleChatDestroyRoom(player, (ChatDestroyRoom) packet);
				break;
			case CHAT_INVITE_AVATAR_TO_ROOM:
				handleChatInviteToRoom(player, (ChatInviteAvatarToRoom) packet);
				break;
			case CHAT_UNINVITE_FROM_ROOM:
				handleChatUninviteFromRoom(player, (ChatUninviteFromRoom) packet);
				break;
			case CHAT_KICK_AVATAR_FROM_ROOM:
				handleChatKickAvatarFromRoom(player, (ChatKickAvatarFromRoom) packet);
				break;
			case CHAT_BAN_AVATAR_FROM_ROOM:
				handleChatBanAvatarFromRoom(player, (ChatBanAvatarFromRoom) packet);
				break;
			case CHAT_UNBAN_AVATAR_FROM_ROOM:
				handleChatUnbanAvatarFromRoom(player, (ChatUnbanAvatarFromRoom) packet);
				break;
			case CHAT_ADD_MODERATOR_TO_ROOM:
				handleChatAddModeratorToRoom(player, (ChatAddModeratorToRoom) packet);
				break;
			case CHAT_REMOVE_MODERATOR_FROM_ROOM:
				handleChatRemoveModeratorFromRoom(player, (ChatRemoveModeratorFromRoom) packet);
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				chatRoomHandler.enterChatChannels(pei.getPlayer());
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleChatRoomUpdateIntent(ChatRoomUpdateIntent crui) {
		switch (crui.getUpdateType()) {
			case CREATE:
				chatRoomHandler.createRoom(crui.getAvatar(), crui.isPublic(), false, crui.getPath(), crui.getTitle(), false);
				chatRoomHandler.enterChatChannel(crui.getPlayer(), crui.getPath(), crui.isIgnoreInvitation());
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
	
	private ChatResult performModeratedChecks(ChatRoom room, Player senderPlayer, ChatAvatar senderAvatar) {
		if (room == null)
			return ChatResult.ROOM_INVALID_NAME;
		
		if (room.getOwner().equals(senderAvatar) || senderPlayer.getAccessLevel().getValue() >= AccessLevel.CSR.getValue())
			return ChatResult.NONE;
		
		if (!room.isModerator(senderAvatar))
			return ChatResult.ROOM_AVATAR_NO_PERMISSION;
		
		return ChatResult.NONE;
	}
	
	private boolean isTargetInvalid(ChatAvatar target) {
		return PlayerLookup.getCharacterByFirstName(target.getName()) == null;
	}
	
	private void handleChatRemoveModeratorFromRoom(Player player, ChatRemoveModeratorFromRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = new ChatAvatar(player.getCharacterChatName());
		ChatAvatar target = p.getAvatar();
		ChatResult result = performModeratedChecks(room, player, sender);
		
		if (result == ChatResult.NONE) {
			assert room != null;
			if (room.removeModerator(target))
				result = ChatResult.SUCCESS;
			else
				result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		}
		
		player.sendPacket(new ChatOnRemoveModeratorFromRoom(target, sender, result.getCode(), p.getRoom(), p.getSequence()));
	}
	
	private void handleChatAddModeratorToRoom(Player player, ChatAddModeratorToRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = new ChatAvatar(player.getCharacterChatName());
		ChatAvatar target = p.getAvatar();
		ChatResult result = performModeratedChecks(room, player, sender);
		
		if (result == ChatResult.NONE) {
			assert room != null;
			if (isTargetInvalid(target)) {
				result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
			} else {
				if (room.removeBanned(target))
					sendPacketToMembers(room, new ChatOnUnbanAvatarFromRoom(p.getRoom(), sender, target, ChatResult.SUCCESS.getCode(), 0));
				room.addModerator(target);
				result = ChatResult.SUCCESS;
			}
		}
		
		player.sendPacket(new ChatOnAddModeratorToRoom(target, sender, result.getCode(), p.getRoom(), p.getSequence()));
	}
	
	private void handleChatUnbanAvatarFromRoom(Player player, ChatUnbanAvatarFromRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = new ChatAvatar(player.getCharacterChatName());
		ChatAvatar target = p.getAvatar();
		ChatResult result = performModeratedChecks(room, player, sender);
		
		if (result == ChatResult.NONE) {
			assert room != null;
			if (room.removeBanned(target))
				result = ChatResult.SUCCESS;
			else
				result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		}
		
		if (room == null)
			player.sendPacket(new ChatOnUnbanAvatarFromRoom(p.getRoom(), sender, target, result.getCode(), p.getSequence()));
		else
			sendPacketToMembers(room, new ChatOnUnbanAvatarFromRoom(p.getRoom(), sender, target, result.getCode(), p.getSequence()));
	}
	
	private void handleChatBanAvatarFromRoom(Player player, ChatBanAvatarFromRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = new ChatAvatar(player.getCharacterChatName());
		ChatAvatar target = p.getAvatar();
		ChatResult result = performModeratedChecks(room, player, sender);
		
		if (result == ChatResult.NONE) {
			assert room != null;
			if (isTargetInvalid(target)) {
				result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
			} else {
				room.removeModerator(target);
				room.removeInvited(target);
				room.addBanned(target);
				result = ChatResult.SUCCESS;
			}
		}
		
		if (room == null)
			player.sendPacket(new ChatOnBanAvatarFromRoom(p.getRoom(), sender, target, result.getCode(), p.getSequence()));
		else
			sendPacketToMembers(room, new ChatOnBanAvatarFromRoom(p.getRoom(), sender, target, result.getCode(), p.getSequence()));
	}
	
	private void handleChatKickAvatarFromRoom(Player player, ChatKickAvatarFromRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = new ChatAvatar(player.getCharacterChatName());
		ChatAvatar target = p.getAvatar();
		ChatResult result = performModeratedChecks(room, player, sender);
		
		if (result == ChatResult.NONE) {
			assert room != null;
			if (room.removeMember(target))
				result = ChatResult.SUCCESS;
			else
				result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		}
		
		if (room == null)
			player.sendPacket(new ChatOnKickAvatarFromRoom(target, sender, result.getCode(), p.getRoom()));
		else
			sendPacketToMembers(room, new ChatOnKickAvatarFromRoom(target, sender, result.getCode(), p.getRoom()));
	}
	
	private void handleChatUninviteFromRoom(Player player, ChatUninviteFromRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = new ChatAvatar(player.getCharacterChatName());
		ChatAvatar target = p.getAvatar();
		ChatResult result = performModeratedChecks(room, player, sender);
		
		if (result == ChatResult.NONE) {
			assert room != null;
			if (room.removeInvited(target))
				result = ChatResult.SUCCESS;
			else
				result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		}
		
		player.sendPacket(new ChatOnUninviteFromRoom(p.getRoom(), sender, target, result.getCode(), p.getSequence()));
	}
	
	private void handleChatInviteToRoom(Player player, ChatInviteAvatarToRoom p) {
		ChatRoom room = chatRoomHandler.getRoomByPath(p.getRoom());
		ChatAvatar sender = new ChatAvatar(player.getCharacterChatName());
		ChatAvatar target = p.getAvatar();
		ChatResult result = performModeratedChecks(room, player, sender);
		
		if (result == ChatResult.NONE) {
			assert room != null;
			if (isTargetInvalid(target)) {
				result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
			} else {
				if (room.addInvited(target)) {
					Player targetPlayer = PlayerLookup.getPlayerByFirstName(target.getName());
					result = ChatResult.SUCCESS;
					targetPlayer.sendPacket(new ChatRoomList(room));
					targetPlayer.sendPacket(new ChatOnReceiveRoomInvitation(sender, p.getRoom()));
				}
			}
		}
		
		player.sendPacket(new ChatOnInviteToRoom(p.getRoom(), sender, target, result.getCode()));
	}
	
	private void handleChatDestroyRoom(Player player, ChatDestroyRoom p) {
		ChatRoom room = chatRoomHandler.getRoomById(p.getRoomId());
		ChatAvatar sender = new ChatAvatar(player.getCharacterChatName());
		ChatResult result;
		
		if (room == null) 
			result = ChatResult.ROOM_INVALID_ID;
		else if (!room.getOwner().equals(sender))
			result = ChatResult.ROOM_AVATAR_NO_PERMISSION;
		else if (chatRoomHandler.notifyDestroyRoom(sender, room.getPath(), p.getSequence()))
			result = ChatResult.SUCCESS;
		else
			result = ChatResult.NONE;
		
		player.sendPacket(new ChatOnDestroyRoom(sender, result.getCode(), p.getRoomId(), p.getSequence()));
	}
	
	private void handleChatCreateRoom(Player player, ChatCreateRoom p) {
		ChatResult result;
		
		if (chatRoomHandler.getRoomByPath(p.getRoomName()) != null) {
			result = ChatResult.ROOM_ALREADY_EXISTS;
		} else {
			if (chatRoomHandler.createRoom(new ChatAvatar(player.getCharacterChatName()), p.isPublic(), p.isModerated(), p.getRoomName(), p.getRoomTitle(), true)) {
				result = ChatResult.SUCCESS;
			} else {
				result = ChatResult.ROOM_ALREADY_EXISTS;
			}
		}
		
		player.sendPacket(new ChatOnCreateRoom(result.getCode(), chatRoomHandler.getRoomByPath(p.getRoomName()), p.getSequence()));
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
	
	private static void sendPacketToMembers(@NotNull ChatRoom room, SWGPacket... packets) {
		for (ChatAvatar member : room.getMembers()) {
			Player player = PlayerLookup.getPlayerByFirstName(member.getName());
			player.sendPacket(packets);
		}
	}
	
}
