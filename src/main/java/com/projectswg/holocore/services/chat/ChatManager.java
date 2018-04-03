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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import com.projectswg.common.control.Manager;
import com.projectswg.common.data.encodables.chat.ChatAvatar;
import com.projectswg.common.data.encodables.chat.ChatResult;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.chat.ChatFriendsListUpdate;
import com.projectswg.common.network.packets.swg.zone.chat.ChatInstantMessageToCharacter;
import com.projectswg.common.network.packets.swg.zone.chat.ChatInstantMessageToClient;
import com.projectswg.common.network.packets.swg.zone.chat.ChatOnSendInstantMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.common.network.packets.swg.zone.object_controller.SpatialChat;

import com.projectswg.holocore.intents.NotifyPlayersPacketIntent;
import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.chat.ChatAvatarRequestIntent;
import com.projectswg.holocore.intents.chat.SpatialChatIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.network.GalacticPacketIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.player.PlayerObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.player.PlayerState;
import com.projectswg.holocore.services.player.PlayerManager.PlayerLookup;

public class ChatManager extends Manager {
	
	private final RelationalServerData chatLogs;
	private final PreparedStatement insertChatLog;
	
	public ChatManager() {
		chatLogs = RelationalServerFactory.getServerDatabase("chat/chat_log.db");
		insertChatLog = chatLogs.prepareStatement("INSERT INTO chat_log VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
		addChildService(new ChatRoomService());
		addChildService(new ChatMailService());
		
		registerForIntent(GalacticPacketIntent.class, this::handleGalacticPacketIntent);
		registerForIntent(SpatialChatIntent.class, this::handleSpatialChatIntent);
		registerForIntent(PlayerEventIntent.class, this::handlePlayerEventIntent);
		registerForIntent(SystemMessageIntent.class, this::handleSystemMessageIntent);
		registerForIntent(ChatAvatarRequestIntent.class, this::handleChatAvatarRequestIntent);
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		switch (packet.getPacketType()) {
			case CHAT_INSTANT_MESSAGE_TO_CHARACTER:
				if (packet instanceof ChatInstantMessageToCharacter)
					handleInstantMessage(gpi.getPlayer(), (ChatInstantMessageToCharacter) packet);
				break;
			default:
				break;
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				updateChatAvatarStatus(pei.getPlayer(), true);
				break;
			case PE_LOGGED_OUT:
				updateChatAvatarStatus(pei.getPlayer(), false);
				break;
			default:
				break;
		}
	}
	
	private void handleSystemMessageIntent(SystemMessageIntent smi) {
		switch (smi.getBroadcastType()) {
			case AREA:
				broadcastAreaMessage(smi.getMessage(), smi.getReceiver());
				logChat(smi.getReceiver(), ChatType.SYSTEM, ChatRange.LOCAL, smi.getMessage());
				break;
			case PLANET:
				broadcastPlanetMessage(smi.getMessage(), smi.getTerrain());
				logChat(smi.getReceiver(), ChatType.SYSTEM, ChatRange.TERRAIN, smi.getMessage());
				break;
			case GALAXY:
				broadcastGalaxyMessage(smi.getMessage());
				logChat(smi.getReceiver(), ChatType.SYSTEM, ChatRange.GALAXY, smi.getMessage());
				break;
			case PERSONAL:
				if (smi.getProse() != null) {
					broadcastPersonalMessage(smi.getReceiver(), smi.getProse());
					logChat(smi.getReceiver(), ChatType.SYSTEM, ChatRange.PERSONAL, "**OOB DATA**");
				} else {
					broadcastPersonalMessage(smi.getReceiver(), smi.getMessage());
					logChat(smi.getReceiver(), ChatType.SYSTEM, ChatRange.PERSONAL, smi.getMessage());
				}
				break;
		}
	}
	
	private void handleChatAvatarRequestIntent(ChatAvatarRequestIntent cari) {
		switch (cari.getRequestType()) {
			case TARGET_STATUS:
				sendTargetAvatarStatus(cari.getPlayer(), new ChatAvatar(cari.getTarget()));
				break;
			case FRIEND_ADD_TARGET:
				handleAddFriend(cari.getPlayer(), cari.getTarget());
				break;
			case FRIEND_REMOVE_TARGET:
				handleRemoveFriend(cari.getPlayer(), cari.getTarget());
				break;
			case FRIEND_LIST:
				handleRequestFriendList(cari.getPlayer());
				break;
			case IGNORE_ADD_TARGET:
				handleAddIgnored(cari.getPlayer(), cari.getTarget());
				break;
			case IGNORE_REMOVE_TARGET:
				handleRemoveIgnored(cari.getPlayer(), cari.getTarget());
				break;
			case IGNORE_LIST:
				handleRequestIgnoreList(cari.getPlayer());
				break;
		}
	}
	
	/* Friends List */
	
	private void handleAddFriend(Player player, String target) {
		if (target.equalsIgnoreCase(player.getCharacterFirstName()))
			return;
		
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;
		
		if (ghost.isIgnored(target)) {
			sendSystemMessage(player, "@cmnty:friend_fail_is_ignored", target);
			return;
		}
		
		if (!player.getPlayerManager().playerExists(target)) {
			sendSystemMessage(player, "@cmnty:friend_not_found", target);
			return;
		}
		
		if (!ghost.addFriend(target)) {
			sendSystemMessage(player, "@cmnty:friend_duplicate", target);
			return;
		}
		
		sendSystemMessage(player, "@cmnty:friend_added", target);
		sendTargetAvatarStatus(player, new ChatAvatar(target));
	}
	
	private void handleRemoveFriend(Player player, String target) {
		if (!player.getPlayerObject().removeFriend(target)) {
			sendSystemMessage(player, "@cmnty:friend_not_found", target);
			return;
		}
		
		sendSystemMessage(player, "@cmnty:friend_removed", target);
	}
	
	private void handleRequestFriendList(Player player) {
		player.getPlayerObject().sendFriendsList();
	}
	
	/* Ignore List */
	
	private void handleAddIgnored(Player player, String target) {
		if (target.equalsIgnoreCase(player.getCharacterFirstName()))
			return;
		
		if (!player.getPlayerManager().playerExists(target)) {
			sendSystemMessage(player, "@cmnty:ignore_not_found", target);
			return;
		}
		
		player.getPlayerObject().removeFriend(target);
		
		if (!player.getPlayerObject().addIgnored(target)) {
			sendSystemMessage(player, "@cmnty:ignore_duplicate", target);
			return;
		}
		
		sendSystemMessage(player, "@cmnty:ignore_added", target);
	}
	
	private void handleRemoveIgnored(Player player, String target) {
		if (!player.getPlayerObject().removeIgnored(target)) {
			sendSystemMessage(player, "@cmnty:ignore_not_found", target);
			return;
		}
		
		sendSystemMessage(player, "@cmnty:ignore_removed", target);
	}
	
	private void handleRequestIgnoreList(Player player) {
		player.getPlayerObject().sendIgnoreList();
	}
	
	/* Misc */
	
	private void sendSystemMessage(Player player, String stringId, String target) {
		new SystemMessageIntent(player, new ProsePackage("StringId", stringId, "TT", target)).broadcast();
	}
	
	private void handleSpatialChatIntent(SpatialChatIntent spi) {
		Player sender = spi.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		String senderName = sender.getCharacterFirstName();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), 0, spi.getMessage(), (short) spi.getChatType(), (short) spi.getMoodId());
		logChat(sender, ChatType.SPATIAL, ChatRange.LOCAL, spi.getMessage());
		
		// Notify observers of the chat message
		for (Player owner : actor.getObservers()) {
			if (owner.getPlayerObject().isIgnored(senderName))
				continue;
			owner.sendPacket(new SpatialChat(owner.getCreatureObject().getObjectId(), message));
		}
	}
	
	private void handleInstantMessage(Player sender, ChatInstantMessageToCharacter request) {
		String strReceiver = request.getCharacter().toLowerCase(Locale.ENGLISH);
		String strSender = sender.getCharacterFirstName();
		
		Player receiver = PlayerLookup.getPlayerByFirstName(strReceiver);
		
		ChatResult result = ChatResult.SUCCESS;
		if (receiver == null || receiver.getPlayerState() != PlayerState.ZONED_IN)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		else if (receiver.getPlayerObject().isIgnored(strSender))
			result = ChatResult.IGNORED;
		
		sender.sendPacket(new ChatOnSendInstantMessage(result.getCode(), request.getSequence()));
		
		if (result != ChatResult.SUCCESS)
			return;
		
		receiver.sendPacket(new ChatInstantMessageToClient(request.getGalaxy(), strSender, request.getMessage()));
		logChat(sender, receiver, ChatType.TELL, request.getMessage());
	}
	
	/* Friends */
	
	private void updateChatAvatarStatus(Player player, boolean online) {
		if (online) {
			for (String friend : player.getPlayerObject().getFriendsList()) {
				sendTargetAvatarStatus(player, new ChatAvatar(friend));
			}
		}
		
		String name = player.getCharacterFirstName().toLowerCase(Locale.US);
		player.getPlayerManager().notifyPlayers(p -> p.getPlayerState() == PlayerState.ZONED_IN && p.getPlayerObject().isFriend(name), new ChatFriendsListUpdate(new ChatAvatar(name), online));
	}
	
	private void sendTargetAvatarStatus(Player player, ChatAvatar target) {
		Player targetPlayer = PlayerLookup.getPlayerByFirstName(target.getName());
		
		player.sendPacket(new ChatFriendsListUpdate(target, targetPlayer != null && targetPlayer.getPlayerState() == PlayerState.ZONED_IN));
	}
	
	private void broadcastAreaMessage(String message, Player broadcaster) {
		broadcaster.getCreatureObject().sendObservers(new ChatSystemMessage(SystemChatType.PERSONAL, message));
	}
	
	private void broadcastPlanetMessage(String message, Terrain terrain) {
		ChatSystemMessage SWGPacket = new ChatSystemMessage(SystemChatType.PERSONAL, message);
		new NotifyPlayersPacketIntent(SWGPacket, terrain).broadcast();
	}
	
	private void broadcastGalaxyMessage(String message) {
		ChatSystemMessage SWGPacket = new ChatSystemMessage(SystemChatType.PERSONAL, message);
		new NotifyPlayersPacketIntent(SWGPacket).broadcast();
	}
	
	private void broadcastPersonalMessage(Player player, String message) {
		player.sendPacket(new ChatSystemMessage(SystemChatType.PERSONAL, message));
	}
	
	private void broadcastPersonalMessage(Player player, ProsePackage prose) {
		player.sendPacket(new ChatSystemMessage(SystemChatType.PERSONAL, new OutOfBandPackage(prose)));
	}
	
	private void logChat(Player broadcaster, ChatType type, ChatRange range, String message) {
		long sendId = 0;
		String sendName = "";
		if (broadcaster != null) {
			CreatureObject creature = broadcaster.getCreatureObject();
			if (creature != null) {
				sendId = creature.getObjectId();
				sendName = creature.getObjectName();
			}
		}
		logChat(sendId, sendName, 0, "", type.name(), range.name(), "", "", message);
	}
	
	private void logChat(Player sender, Player receiver, ChatType type, String message) {
		long sendId = 0, recvId = 0;
		String sendName = "", recvName = "";
		if (sender != null) {
			CreatureObject creature = sender.getCreatureObject();
			if (creature != null) {
				sendId = creature.getObjectId();
				sendName = creature.getObjectName();
			}
		}
		if (receiver != null) {
			CreatureObject creature = receiver.getCreatureObject();
			if (creature != null) {
				recvId = creature.getObjectId();
				recvName = creature.getObjectName();
			}
		}
		logChat(sendId, sendName, recvId, recvName, type.name(), ChatRange.PERSONAL.name(), "", "", message);
	}
	
	private void logChat(long sendId, String sendName, long recvId, String recvName, String type, String range, String room, String subject, String message) {
		Assert.notNull(message, "Message cannot be null!");
		try {
			synchronized (insertChatLog) {
				insertChatLog.setLong(1, System.currentTimeMillis());
				insertChatLog.setLong(2, sendId);
				insertChatLog.setString(3, sendName);
				insertChatLog.setLong(4, recvId);
				insertChatLog.setString(5, recvName);
				insertChatLog.setString(6, type);
				insertChatLog.setString(7, range);
				insertChatLog.setString(8, room);
				insertChatLog.setString(9, subject);
				insertChatLog.setString(10, message);
				insertChatLog.executeUpdate();
			}
		} catch (SQLException e) {
			Log.e(e);
		}
	}
	
	public enum ChatType {
		MAIL, TELL, SYSTEM, SPATIAL, CHAT
	}
	
	public enum ChatRange {
		PERSONAL, ROOM, LOCAL, TERRAIN, GALAXY
	}
	
}
