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

import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import intents.chat.ChatAvatarRequestIntent;
import intents.chat.ChatBroadcastIntent;
import intents.chat.PersistentMessageIntent;
import intents.chat.SpatialChatIntent;
import intents.network.GalacticPacketIntent;
import intents.player.ZonePlayerSwapIntent;
import intents.server.ServerStatusIntent;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.chat.*;
import network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import network.packets.swg.zone.object_controller.SpatialChat;
import resources.Galaxy;
import resources.Terrain;
import resources.chat.ChatAvatar;
import resources.chat.ChatResult;
import resources.collections.SWGList;
import resources.control.Intent;
import resources.control.Manager;
import resources.encodables.OutOfBandPackage;
import resources.encodables.ProsePackage;
import resources.encodables.player.Mail;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.player.PlayerState;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.ObjectDatabase;
import services.player.PlayerManager;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ChatManager extends Manager {

	private final ObjectDatabase<Mail> mails;
	private final ChatRoomService roomService;
	private int maxMailId;

	public ChatManager(Galaxy g) {
		roomService = new ChatRoomService(g);
		mails = new CachedObjectDatabase<>("odb/mails.db");
		maxMailId = 1;

		addChildService(roomService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(SpatialChatIntent.TYPE);
		registerForIntent(PersistentMessageIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ChatBroadcastIntent.TYPE);
		registerForIntent(ServerStatusIntent.TYPE);
		registerForIntent(ChatAvatarRequestIntent.TYPE);
		registerForIntent(ZonePlayerSwapIntent.TYPE);

		mails.load();
		mails.traverse(mail -> {
			if (mail.getId() >= maxMailId)
				maxMailId = mail.getId() + 1;
		});
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		mails.close();
		return super.terminate();
	}
	
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					processPacket((GalacticPacketIntent) i);
				break;
			case SpatialChatIntent.TYPE:
				if (i instanceof SpatialChatIntent)
					handleSpatialChat((SpatialChatIntent) i);
				break;
			case PersistentMessageIntent.TYPE:
				if (i instanceof PersistentMessageIntent)
					handlePersistentMessageIntent((PersistentMessageIntent) i);
				break;
			case PlayerEventIntent.TYPE:
				if (i instanceof PlayerEventIntent)
					handlePlayerEventIntent((PlayerEventIntent) i);
				break;
			case ChatBroadcastIntent.TYPE:
				if (i instanceof ChatBroadcastIntent)
					handleChatBroadcast((ChatBroadcastIntent) i);
				break;
			case ChatAvatarRequestIntent.TYPE:
				if (i instanceof ChatAvatarRequestIntent)
					handleChatAvatarStatusRequestIntent((ChatAvatarRequestIntent) i);
				break;
		}
	}

	private void processPacket(GalacticPacketIntent intent) {
		Player player = intent.getPlayerManager().getPlayerFromNetworkId(intent.getNetworkId());
		if (player == null)
			return;

		Packet p = intent.getPacket();
		if (p instanceof SWGPacket)
			processSwgPacket(intent.getPlayerManager(), player, intent.getGalaxy().getName(), (SWGPacket) p);
	}
	
	private void processSwgPacket(PlayerManager pm, Player player, String galaxyName, SWGPacket p) {
		switch (p.getPacketType()) {
			/* Mails */
			case CHAT_PERSISTENT_MESSAGE_TO_SERVER:
				if (p instanceof ChatPersistentMessageToServer)
					handleSendPersistentMessage(pm, player, galaxyName, (ChatPersistentMessageToServer) p);
				break;
			case CHAT_REQUEST_PERSISTENT_MESSAGE:
				if (p instanceof ChatRequestPersistentMessage)
					handlePersistentMessageRequest(player, galaxyName, (ChatRequestPersistentMessage) p);
				break;
			case CHAT_DELETE_PERSISTENT_MESSAGE:
				if (p instanceof ChatDeletePersistentMessage)
					deletePersistentMessage(((ChatDeletePersistentMessage) p).getMailId());
				break;
			/* Misc */
			case CHAT_INSTANT_MESSAGE_TO_CHARACTER:
				if (p instanceof ChatInstantMessageToCharacter)
					handleInstantMessage(pm, player, (ChatInstantMessageToCharacter) p);
				break;
			default: break;
		}
	}

	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		Player player = intent.getPlayer();
		if (player == null)
			return;

		switch (intent.getEvent()) {
			case PE_ZONE_IN:
				roomService.enterPlanetaryChatChannels(player);
				break;
			case PE_FIRST_ZONE:
				sendPersistentMessageHeaders(player, intent.getGalaxy());
				updateChatAvatarStatus(player, true);
				if (player.getPlayerObject() != null)
					roomService.enterChatChannels(player, player.getPlayerObject().getJoinedChannels());
				break;
			case PE_LOGGED_OUT:
				if (player.getCreatureObject() == null)
					break;
				updateChatAvatarStatus(player, false);
				break;
			default: break;
		}
	}

	private void handleChatBroadcast(ChatBroadcastIntent i) {
		switch(i.getBroadcastType()) {
			case AREA:
				broadcastAreaMessage(i.getMessage(), i.getBroadcaster());
				break;
			case PLANET:
				broadcastPlanetMessage(i.getMessage(), i.getTerrain());
				break;
			case GALAXY:
				broadcastGalaxyMessage(i.getMessage());
				break;
			case PERSONAL:
				broadcastPersonalMessage(i.getProse(), i.getBroadcaster(), i.getMessage());
				break;
		}
	}

	private void handleChatAvatarStatusRequestIntent(ChatAvatarRequestIntent i) {
		switch (i.getRequestType()) {
			case TARGET_STATUS: {
				Player player = i.getPlayer();
				sendTargetAvatarStatus(player, new ChatAvatar(0, i.getTarget(), player.getGalaxyName()));
				break;
			}
			case FRIEND_ADD_TARGET:
				handleAddFriend(i.getPlayer(), i.getTarget());
				break;
			case FRIEND_REMOVE_TARGET:
				handleRemoveFriend(i.getPlayer(), i.getTarget());
				break;
			case FRIEND_LIST:
				handleRequestFriendList(i.getPlayer());
				break;
			case IGNORE_REMOVE_TARGET:
				handleRemoveIgnored(i.getPlayer(), i.getTarget());
				break;
			case IGNORE_ADD_TARGET:
				handleAddIgnored(i.getPlayer(), i.getTarget());
				break;
			case IGNORE_LIST:
				handleRequestIgnoreList(i.getPlayer());
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

		if (!ghost.getFriendsList().contains(target)) {
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

		if (ghost.getFriendsList().contains(target)) {
			sendSystemMessage(player, "@cmnty:friend_duplicate", target);
			return;
		}

		if (!player.getPlayerManager().playerExists(target)) {
			sendSystemMessage(player, "@cmnty:friend_not_found", target);
			return;
		}

		ghost.addFriend(target);
		sendSystemMessage(player, "@cmnty:friend_added", target);

		Player targetPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(target);

		if (targetPlayer != null && targetPlayer.getPlayerState() == PlayerState.ZONED_IN)
			player.sendPacket(new ChatFriendsListUpdate(new ChatAvatar(0, target, targetPlayer.getGalaxyName()), true));
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

	private void handleSpatialChat(SpatialChatIntent i) {
		Player sender = i.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), 0, i.getMessage(), (short) i.getChatType(), (short) i.getMoodId());
		sender.sendPacket(message);

		String senderName = ChatAvatar.getFromPlayer(sender).getName();

		// Notify observers of the chat message
		for (SWGObject aware : actor.getObservers()) {
			Player owner = aware.getOwner();
			if (owner == null)
				continue;

			PlayerObject awareGhost = owner.getPlayerObject();
			if (!awareGhost.isIgnored(senderName))
				aware.getOwner().sendPacket(new SpatialChat(aware.getObjectId(), message));
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
	}

	/* Mails */

	private void handleSendPersistentMessage(PlayerManager playerMgr, Player sender, String galaxy, ChatPersistentMessageToServer request) {
		String recipientStr = request.getRecipient().toLowerCase(Locale.ENGLISH);
		
		if (recipientStr.contains(" "))
			recipientStr = recipientStr.split(" ")[0];
		
		Player recipient = playerMgr.getPlayerByCreatureFirstName(recipientStr);
		long recId = (recipient == null ? playerMgr.getCharacterIdByName(request.getRecipient()) : recipient.getCreatureObject().getObjectId());
		ChatResult result = ChatResult.SUCCESS;
		
		if (recId == 0)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;

		if (sender.getPlayerObject().isIgnored(recipientStr))
			result = ChatResult.IGNORED;

		sender.sendPacket(new ChatOnSendPersistentMessage(result, request.getCounter()));

		if (result != ChatResult.SUCCESS)
			return;

		Mail mail = new Mail(sender.getCharacterName().split(" ")[0].toLowerCase(), request.getSubject(), request.getMessage(), recId);
		mail.setId(maxMailId++);
		mail.setTimestamp((int) (new Date().getTime() / 1000));
		mail.setOutOfBandPackage(request.getOutOfBandPackage());
		mails.put(mail.getId(), mail);
		
		if (recipient != null)
			sendPersistentMessage(recipient, mail, MailFlagType.HEADER_ONLY, galaxy);
	}
	
	private void handlePersistentMessageIntent(PersistentMessageIntent intent) {
		if (intent.getReceiver() == null)
			return;
		
		Player recipient = intent.getReceiver().getOwner();
		
		if (recipient == null)
			return;
		
		Mail mail = intent.getMail();
		mail.setId(maxMailId);
		maxMailId++;
		mail.setTimestamp((int) (new Date().getTime() / 1000));
		
		mails.put(mail.getId(), mail);
		
		sendPersistentMessage(recipient, mail, MailFlagType.HEADER_ONLY, intent.getGalaxy());
	}
	
	private void handlePersistentMessageRequest(Player player, String galaxy, ChatRequestPersistentMessage request) {
		Mail mail = mails.get(request.getMailId());
		
		if (mail == null)
			return;
		
		if (mail.getReceiverId() != player.getCreatureObject().getObjectId())
			return;
		
		mail.setStatus(Mail.READ);
		sendPersistentMessage(player, mail, MailFlagType.FULL_MESSAGE, galaxy);
	}

	/* Friends */

	private void updateChatAvatarStatus(Player player, boolean online) {
		PlayerManager playerManager = player.getPlayerManager();
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);
		String galaxy = player.getGalaxyName();

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
	
	private void sendPersistentMessageHeaders(Player player, String galaxy) {
		if (player == null || player.getCreatureObject() == null)
			return;
		
		final List <Mail> playersMail = new LinkedList<>();
		final long receiverId = player.getCreatureObject().getObjectId();

		mails.traverse(element -> {
			if (element.getReceiverId() == receiverId)
				playersMail.add(element);
		});
		
		for (Mail mail : playersMail)
			sendPersistentMessage(player, mail, MailFlagType.HEADER_ONLY, galaxy);
	}
	
	private void sendPersistentMessage(Player receiver, Mail mail, MailFlagType requestType, String galaxy) {
		if (receiver == null || receiver.getCreatureObject() == null)
			return;

		PlayerObject ghost = receiver.getPlayerObject();
		if (ghost.isIgnored(mail.getSender())) {
			mails.remove(mail.getId());
			return;
		}

		ChatPersistentMessageToClient packet = null;
		
		switch (requestType) {
			case FULL_MESSAGE:
				packet = new ChatPersistentMessageToClient(mail, galaxy, false);
				break;
			case HEADER_ONLY:
				packet = new ChatPersistentMessageToClient(mail, galaxy, true);
				break;
		}
		
		receiver.sendPacket(packet);
	}
	
	private void deletePersistentMessage(int mailId) {
		mails.remove(mailId);
	}

	private enum MailFlagType {
		FULL_MESSAGE,
		HEADER_ONLY
	}
}
