/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package services.chat;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import com.projectswg.common.control.Manager;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;

import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import intents.chat.ChatAvatarRequestIntent;
import intents.chat.ChatBroadcastIntent;
import intents.chat.SpatialChatIntent;
import intents.network.GalacticPacketIntent;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.chat.ChatFriendsListUpdate;
import network.packets.swg.zone.chat.ChatInstantMessageToCharacter;
import network.packets.swg.zone.chat.ChatInstantMessageToClient;
import network.packets.swg.zone.chat.ChatOnSendInstantMessage;
import network.packets.swg.zone.chat.ChatSystemMessage;
import network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import network.packets.swg.zone.object_controller.SpatialChat;
import resources.chat.ChatAvatar;
import resources.chat.ChatResult;
import resources.encodables.OutOfBandPackage;
import resources.encodables.ProsePackage;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.player.PlayerState;

public class ChatManager extends Manager {
	
	private final ChatRoomService roomService;
	private final ChatMailService mailService;
	private final RelationalServerData chatLogs;
	private final PreparedStatement insertChatLog;
	
	public ChatManager() {
		roomService = new ChatRoomService();
		mailService = new ChatMailService();
		chatLogs = RelationalServerFactory.getServerDatabase("chat/chat_log.db");
		insertChatLog = chatLogs.prepareStatement("INSERT INTO chat_log VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
		addChildService(roomService);
		addChildService(mailService);
		
		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
		registerForIntent(SpatialChatIntent.class, spi -> handleSpatialChatIntent(spi));
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
		registerForIntent(ChatBroadcastIntent.class, cbi -> handleChatBroadcastIntent(cbi));
		registerForIntent(ChatAvatarRequestIntent.class, cari -> handleChatAvatarRequestIntent(cari));
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet p = gpi.getPacket();
		if (!(p instanceof SWGPacket))
			return;
		switch (((SWGPacket) p).getPacketType()) {
			case CHAT_INSTANT_MESSAGE_TO_CHARACTER:
				if (p instanceof ChatInstantMessageToCharacter)
					handleInstantMessage(gpi.getPlayer(), (ChatInstantMessageToCharacter) p);
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
	
	private void handleChatBroadcastIntent(ChatBroadcastIntent cbi) {
		switch (cbi.getBroadcastType()) {
			case AREA:
				broadcastAreaMessage(cbi.getMessage(), cbi.getBroadcaster());
				logChat(cbi.getBroadcaster(), ChatType.SYSTEM, ChatRange.LOCAL, cbi.getMessage());
				break;
			case PLANET:
				broadcastPlanetMessage(cbi.getMessage(), cbi.getTerrain());
				logChat(cbi.getBroadcaster(), ChatType.SYSTEM, ChatRange.TERRAIN, cbi.getMessage());
				break;
			case GALAXY:
				broadcastGalaxyMessage(cbi.getMessage());
				logChat(cbi.getBroadcaster(), ChatType.SYSTEM, ChatRange.GALAXY, cbi.getMessage());
				break;
			case PERSONAL:
				if (cbi.getProse() != null) {
					broadcastPersonalMessage(cbi.getBroadcaster(), cbi.getProse());
					logChat(cbi.getBroadcaster(), ChatType.SYSTEM, ChatRange.PERSONAL, "**OOB DATA**");
				} else {
					broadcastPersonalMessage(cbi.getBroadcaster(), cbi.getMessage());
					logChat(cbi.getBroadcaster(), ChatType.SYSTEM, ChatRange.PERSONAL, cbi.getMessage());
				}
				break;
		}
	}
	
	private void handleChatAvatarRequestIntent(ChatAvatarRequestIntent cari) {
		switch (cari.getRequestType()) {
			case TARGET_STATUS:
				sendTargetAvatarStatus(cari.getPlayer(), new ChatAvatar(cari.getPlayer(), cari.getTarget()));
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
		sendTargetAvatarStatus(player, new ChatAvatar(null, target));
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
		new ChatBroadcastIntent(player, new ProsePackage("StringId", stringId, "TT", target)).broadcast();
	}
	
	private void handleSpatialChatIntent(SpatialChatIntent spi) {
		Player sender = spi.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		String senderName = sender.getCharacterFirstName();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), 0, spi.getMessage(), (short) spi.getChatType(), (short) spi.getMoodId());
		sender.sendPacket(message);
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
		
		Player receiver = sender.getPlayerManager().getPlayerByCreatureFirstName(strReceiver);
		
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
				sendTargetAvatarStatus(player, new ChatAvatar(null, friend));
			}
		}
		
		String playerName = player.getCharacterFirstName().toLowerCase(Locale.US);
		player.getPlayerManager().notifyPlayers(playerNotified -> {
			if (playerNotified.getPlayerState() != PlayerState.ZONED_IN)
				return false;
			
			return playerNotified.getPlayerObject().isFriend(playerName);
		}, new ChatFriendsListUpdate(new ChatAvatar(player, playerName), online));
	}
	
	private void sendTargetAvatarStatus(Player player, ChatAvatar target) {
		Player targetPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(target.getName());
		
		player.sendPacket(new ChatFriendsListUpdate(target, targetPlayer != null && targetPlayer.getPlayerState() == PlayerState.ZONED_IN));
	}
	
	private void broadcastAreaMessage(String message, Player broadcaster) {
		broadcaster.getCreatureObject().sendObserversAndSelf(new ChatSystemMessage(SystemChatType.PERSONAL, message));
	}
	
	private void broadcastPlanetMessage(String message, Terrain terrain) {
		ChatSystemMessage packet = new ChatSystemMessage(SystemChatType.PERSONAL, message);
		new NotifyPlayersPacketIntent(packet, terrain).broadcast();
	}
	
	private void broadcastGalaxyMessage(String message) {
		ChatSystemMessage packet = new ChatSystemMessage(SystemChatType.PERSONAL, message);
		new NotifyPlayersPacketIntent(packet).broadcast();
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
			sendId = broadcaster.getCreatureObject().getObjectId();
			sendName = broadcaster.getCharacterName();
		}
		logChat(sendId, sendName, 0, "", type.name(), range.name(), "", "", message);
	}
	
	private void logChat(Player sender, Player receiver, ChatType type, String message) {
		long sendId = 0, recvId = 0;
		String sendName = "", recvName = "";
		if (sender != null) {
			sendId = sender.getCreatureObject().getObjectId();
			sendName = sender.getCharacterName();
		}
		if (receiver != null) {
			recvId = receiver.getCreatureObject().getObjectId();
			recvName = receiver.getCharacterName();
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
