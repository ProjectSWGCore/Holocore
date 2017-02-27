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
import resources.Terrain;
import resources.chat.ChatAvatar;
import resources.chat.ChatResult;
import resources.collections.SWGList;
import resources.control.Manager;
import resources.encodables.OutOfBandPackage;
import resources.encodables.ProsePackage;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.player.PlayerState;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;
import services.CoreManager;
import services.player.PlayerManager;

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
		SWGPacket swg = (SWGPacket) p;
		switch (swg.getPacketType()) {
			case CHAT_INSTANT_MESSAGE_TO_CHARACTER:
				if (p instanceof ChatInstantMessageToCharacter)
					handleInstantMessage(gpi.getPlayerManager(), gpi.getPlayer(), (ChatInstantMessageToCharacter) p);
				break;
			default:
				break;
		}
	}

	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player player = pei.getPlayer();
		if (player == null)
			return;

		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				updateChatAvatarStatus(player, true);
				break;
			case PE_LOGGED_OUT:
				if (player.getCreatureObject() == null)
					break;
				updateChatAvatarStatus(player, false);
				break;
			default: break;
		}
	}

	private void handleChatBroadcastIntent(ChatBroadcastIntent cbi) {
		switch(cbi.getBroadcastType()) {
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
				broadcastPersonalMessage(cbi.getProse(), cbi.getBroadcaster(), cbi.getMessage());
				logChat(cbi.getBroadcaster(), ChatType.SYSTEM, ChatRange.PERSONAL, cbi.getMessage());
				break;
		}
	}

	private void handleChatAvatarRequestIntent(ChatAvatarRequestIntent cari) {
		switch (cari.getRequestType()) {
			case TARGET_STATUS: {
				Player player = cari.getPlayer();
				sendTargetAvatarStatus(player, new ChatAvatar(0, cari.getTarget(), CoreManager.getGalaxy().getName()));
				break;
			}
			case FRIEND_ADD_TARGET:
				handleAddFriend(cari.getPlayer(), cari.getTarget());
				break;
			case FRIEND_REMOVE_TARGET:
				handleRemoveFriend(cari.getPlayer(), cari.getTarget());
				break;
			case FRIEND_LIST:
				handleRequestFriendList(cari.getPlayer());
				break;
			case IGNORE_REMOVE_TARGET:
				handleRemoveIgnored(cari.getPlayer(), cari.getTarget());
				break;
			case IGNORE_ADD_TARGET:
				handleAddIgnored(cari.getPlayer(), cari.getTarget());
				break;
			case IGNORE_LIST:
				handleRequestIgnoreList(cari.getPlayer());
				break;
		}
	}

	/* Friends List */

	private void handleRequestFriendList(Player player) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		SWGList<String> friends = (SWGList<String>) ghost.getFriendsList();

		friends.sendRefreshedListData(ghost);
	}

	private void handleRemoveFriend(Player player, String target) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		if (!ghost.isFriend(target)) {
			sendSystemMessage(player, "@cmnty:friend_not_found", target);
			return;
		}

		ghost.removeFriend(target);

		sendSystemMessage(player, "@cmnty:friend_removed", target);
	}

	private void handleAddFriend(Player player, String target) {
		if (target.equalsIgnoreCase(player.getCharacterName().split(" ")[0]))
			return;

		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		if (ghost.isIgnored(target)) {
			sendSystemMessage(player, "@cmnty:friend_fail_is_ignored", target);
			return;
		}

		if (ghost.isFriend(target)) {
			sendSystemMessage(player, "@cmnty:friend_duplicate", target);
			return;
		}

		if (!player.getPlayerManager().playerExists(target)) {
			sendSystemMessage(player, "@cmnty:friend_not_found", target);
			return;
		}

		ghost.addFriend(target);
		sendSystemMessage(player, "@cmnty:friend_added", target);

		player.sendPacket(new ChatFriendsListUpdate(new ChatAvatar(0, target, CoreManager.getGalaxy().getName()), true));
	}

	/* Ignore List */

	private void handleAddIgnored(Player player, String target) {
		if (target.equalsIgnoreCase(player.getCharacterName().split(" ")[0]))
			return;

		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		if (ghost.isIgnored(target)) {
			sendSystemMessage(player, "@cmnty:ignore_duplicate", target);
			return;
		}

		if (!player.getPlayerManager().playerExists(target)) {
			sendSystemMessage(player, "@cmnty:ignore_not_found", target);
			return;
		}

		ghost.addIgnored(target);
		sendSystemMessage(player, "@cmnty:ignore_added", target);
	}

	private void handleRemoveIgnored(Player player, String target) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		if (!ghost.isIgnored(target)) {
			sendSystemMessage(player, "@cmnty:ignore_not_found", target);
			return;
		}

		ghost.removeIgnored(target);

		sendSystemMessage(player, "@cmnty:ignore_removed", target);
	}

	private void handleRequestIgnoreList(Player player) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		SWGList<String> ignored = (SWGList<String>) ghost.getIgnoreList();

		ignored.sendRefreshedListData(ghost);
	}

	/* Misc */

	private void sendSystemMessage(Player player, String stringId, String target) {
		new ChatBroadcastIntent(player, new ProsePackage("StringId", stringId, "TT", target)).broadcast();
	}

	private void handleSpatialChatIntent(SpatialChatIntent spi) {
		Player sender = spi.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), 0, spi.getMessage(), (short) spi.getChatType(), (short) spi.getMoodId());
		sender.sendPacket(message);
		logChat(sender, ChatType.SPATIAL, ChatRange.LOCAL, spi.getMessage());

		String senderName = ChatAvatar.getFromPlayer(sender).getName();

		// Notify observers of the chat message
		for (Player owner : actor.getObservers()) {
			PlayerObject awareGhost = owner.getPlayerObject();
			if (!awareGhost.isIgnored(senderName))
				owner.sendPacket(new SpatialChat(owner.getCreatureObject().getObjectId(), message));
		}
	}
	
	private void handleInstantMessage(PlayerManager playerMgr, Player sender, ChatInstantMessageToCharacter request) {
		String strReceiver = request.getCharacter().toLowerCase(Locale.ENGLISH);
		String strSender = sender.getCharacterName();
		
		if (strSender.contains(" "))
			strSender = strSender.split(" ")[0];
		
		Player receiver = playerMgr.getPlayerByCreatureFirstName(strReceiver);
		
		int errorCode = 0; // 0 = No issue, 4 = "strReceiver is not online"
		if (receiver == null || receiver.getPlayerState() != PlayerState.ZONED_IN)
			errorCode = 4;

		if (errorCode != 4 && receiver.getPlayerObject() != null && receiver.getPlayerObject().isIgnored(strSender))
			errorCode = ChatResult.IGNORED.getCode();

		sender.sendPacket(new ChatOnSendInstantMessage(errorCode, request.getSequence()));
		
		if (errorCode != 0)
			return;
		
		receiver.sendPacket(new ChatInstantMessageToClient(request.getGalaxy(), strSender, request.getMessage()));
		logChat(sender, receiver, ChatType.TELL, request.getMessage());
	}

	/* Friends */

	private void updateChatAvatarStatus(Player player, boolean online) {
		PlayerManager playerManager = player.getPlayerManager();
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);
		String galaxy = CoreManager.getGalaxy().getName();

		if (online) {
			PlayerObject playerObject = player.getPlayerObject();
			if (playerObject != null && playerObject.getFriendsList().size() <= 0) {
				for (String friend : playerObject.getFriendsList()) {
					sendTargetAvatarStatus(player, new ChatAvatar(0, friend, galaxy));
				}
			}
		}

		final ChatFriendsListUpdate update = new ChatFriendsListUpdate(avatar, online);

		playerManager.notifyPlayers(playerNotified -> {
			if (playerNotified.getPlayerState() != PlayerState.ZONED_IN)
				return false;

			PlayerObject playerObject = playerNotified.getPlayerObject();
			return playerObject != null && playerObject.getFriendsList().contains(update.getFriend().getName());

		}, update);
	}

	private void sendTargetAvatarStatus(Player player, ChatAvatar target) {
		PlayerObject object = player.getPlayerObject();
		if (object == null)
			return;

		Player targetPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(target.getName());

		boolean online = true;
		if (targetPlayer == null || targetPlayer.getPlayerState() != PlayerState.ZONED_IN)
			online = false;

		player.sendPacket(new ChatFriendsListUpdate(target, online));
	}

	private void broadcastAreaMessage(String message, Player broadcaster) {
		ChatSystemMessage packet = new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT.ordinal(), message);
		broadcaster.sendPacket(packet);
		
		broadcaster.getCreatureObject().sendObservers(packet);
	}

	private void broadcastPlanetMessage(String message, Terrain terrain) {
		ChatSystemMessage packet = new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT.ordinal(), message);
		new NotifyPlayersPacketIntent(packet, terrain, null).broadcast();
	}

	private void broadcastGalaxyMessage(String message) {
		ChatSystemMessage packet = new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT.ordinal(), message);
		new NotifyPlayersPacketIntent(packet).broadcast();
	}
	
	private void broadcastPersonalMessage(ProsePackage prose, Player player, String message) {
		if (prose != null)
			player.sendPacket(new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT, new OutOfBandPackage(prose)));
		else
			player.sendPacket(new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT, message));
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
		if (message == null)
			return;
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
	
	public static enum ChatType {
		MAIL,
		TELL,
		SYSTEM,
		SPATIAL,
		CHAT
	}
	
	public static enum ChatRange {
		PERSONAL,
		ROOM,
		LOCAL,
		TERRAIN,
		GALAXY
	}
	
}
