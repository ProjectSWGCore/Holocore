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
import network.packets.swg.zone.ChatRequestRoomList;
import network.packets.swg.zone.chat.ChatDeletePersistentMessage;
import network.packets.swg.zone.chat.ChatFriendsListUpdate;
import network.packets.swg.zone.chat.ChatInstantMessageToCharacter;
import network.packets.swg.zone.chat.ChatInstantMessageToClient;
import network.packets.swg.zone.chat.ChatOnAddFriend;
import network.packets.swg.zone.chat.ChatOnChangeFriendStatus;
import network.packets.swg.zone.chat.ChatOnGetFriendsList;
import network.packets.swg.zone.chat.ChatOnSendInstantMessage;
import network.packets.swg.zone.chat.ChatOnSendPersistentMessage;
import network.packets.swg.zone.chat.ChatPersistentMessageToClient;
import network.packets.swg.zone.chat.ChatPersistentMessageToServer;
import network.packets.swg.zone.chat.ChatRequestPersistentMessage;
import network.packets.swg.zone.chat.ChatSystemMessage;
import network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import network.packets.swg.zone.object_controller.SpatialChat;
import resources.Terrain;
import resources.collections.SWGList;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.OutOfBand;
import resources.encodables.ProsePackage;
import resources.encodables.player.Mail;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.player.PlayerState;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;
import services.player.PlayerManager;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ChatService extends Service {
	
	private ObjectDatabase<Mail> mails;
	private int maxMailId;
	
	public ChatService() {
		mails = new CachedObjectDatabase<>("odb/mails.db");
		maxMailId = 1;
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
		mails.traverse(new Traverser<Mail>() {
			@Override
			public void process(Mail mail) {
				if (mail.getId() >= maxMailId)
					maxMailId = mail.getId() + 1;
			}
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
			case CHAT_REQUEST_ROOM_LIST:
				if (p instanceof ChatRequestRoomList)
					handleChatRoomListRequest(player, (ChatRequestRoomList) p);
				break;
			case CHAT_INSTANT_MESSAGE_TO_CHARACTER:
				if (p instanceof ChatInstantMessageToCharacter)
					handleInstantMessage(pm, player, (ChatInstantMessageToCharacter) p);
				break;
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
			default:
				break;
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		switch (intent.getEvent()) {
			case PE_FIRST_ZONE:
				Player player = intent.getPlayer();
				sendPersistentMessageHeaders(player, intent.getGalaxy());
				updateChatAvatarStatus(player, intent.getGalaxy(), true);
				break;
			case PE_LOGGED_OUT:
				if (intent.getPlayer() == null || intent.getPlayer().getCreatureObject() == null)
					return;
				updateChatAvatarStatus(intent.getPlayer(), intent.getGalaxy(), false);
				break;
			default:
				break;
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
			case TARGET_STATUS:
				sendTargetAvatarStatus(i.getPlayer(), i.getTarget());
				break;
			case IGNORE_REMOVE_TARGET:
				break;
			case IGNORE_ADD_TARGET:
				break;
			case FRIEND_ADD_TARGET:
				handleAddFriend(i.getPlayer(), i.getTarget());
				break;
			case FRIEND_REMOVE_TARGET:
				handleRemoveFriend(i.getPlayer(), i.getTarget());
				break;
			case FRIEND_LIST:
				handleRequestFriendList(i.getPlayer());
				break;
		}
	}

	private void handleRequestFriendList(Player player) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		SWGList<String> friends = (SWGList<String>) ghost.getFriendsList();
		player.sendPacket(new ChatOnGetFriendsList(player.getCreatureObject().getObjectId(), player.getGalaxyName(), friends));

		friends.sendRefreshedListData(ghost);
	}

	private void handleRemoveFriend(Player player, String target) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		ChatOnChangeFriendStatus friendStatus = new ChatOnChangeFriendStatus(
				player.getCreatureObject().getObjectId(), player.getGalaxyName(), target, true);

		player.sendPacket(friendStatus);

		ghost.removeFriend(target);

		new ChatBroadcastIntent(player, new ProsePackage("@cmnty:friend_removed", "TT", target)).broadcast();
	}

	private void handleAddFriend(Player player, String target) {
		if (target.equalsIgnoreCase(player.getCharacterName().split(" ")[0]))
			return;

		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		if (ghost.getFriendsList().contains(target)) {
			new ChatBroadcastIntent(player, new ProsePackage("@cmnty:friend_duplicate", "TT", target)).broadcast();
			return;
		}

		if (!player.getPlayerManager().playerExists(target)) {
			new ChatBroadcastIntent(player, new ProsePackage("@cmnty:friend_not_found", "TT", target)).broadcast();
			return;
		}

		ChatOnChangeFriendStatus friendStatus = new ChatOnChangeFriendStatus(
				player.getCreatureObject().getObjectId(), player.getGalaxyName(), target, false);

		player.sendPacket(new ChatOnAddFriend(), friendStatus);

		Player targetPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(target);
		if (targetPlayer != null && targetPlayer.getPlayerState() == PlayerState.ZONED_IN)
			player.sendPacket(new ChatFriendsListUpdate(player.getGalaxyName(), target, true));

		ghost.addFriend(target);

		new ChatBroadcastIntent(player, new ProsePackage("@cmnty:friend_added", "TT", target)).broadcast();
	}

	private void handleChatRoomListRequest(Player player, ChatRequestRoomList request) {

	}

	private void handleSpatialChat(SpatialChatIntent i) {
		Player sender = i.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), 0, i.getMessage(), (short) i.getChatType(), (short) i.getMoodId());
		sender.sendPacket(message);
		
		// Notify observers of the chat message
		for (SWGObject aware : actor.getObjectsAware()) {
			if (aware.getOwner() != null)
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
		
		sender.sendPacket(new ChatOnSendInstantMessage(errorCode, request.getSequence()));
		
		if (errorCode == 4)
			return;
		
		receiver.sendPacket(new ChatInstantMessageToClient(request.getGalaxy(), strSender, request.getMessage()));
	}
	
	private void handleSendPersistentMessage(PlayerManager playerMgr, Player sender, String galaxy, ChatPersistentMessageToServer request) {
		String recipientStr = request.getRecipient().toLowerCase(Locale.ENGLISH);
		
		if (recipientStr.contains(" "))
			recipientStr = recipientStr.split(" ")[0];
		
		Player recipient = playerMgr.getPlayerByCreatureFirstName(recipientStr);
		long recId = (recipient == null ? playerMgr.getCharacterIdByName(request.getRecipient()) : recipient.getCreatureObject().getObjectId());
		int errorCode = 0;
		
		if (recId == 0)
			errorCode = 4;
		
		sender.sendPacket(new ChatOnSendPersistentMessage(errorCode, request.getCounter()));
		
		Mail mail = new Mail(sender.getCharacterName(), request.getSubject(), request.getMessage(), recId);
		mail.setId(maxMailId++);
		mail.setTimestamp((int) (new Date().getTime() / 1000));
		
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

	private void updateChatAvatarStatus(Player player, String galaxy, boolean online) {
		PlayerManager playerManager = player.getPlayerManager();
		String firstName = player.getCharacterName().toLowerCase();
		if (firstName.contains(" "))
			firstName = firstName.substring(0, firstName.indexOf(' '));

		if (online) {
			PlayerObject playerObject = player.getPlayerObject();
			if (playerObject != null && playerObject.getFriendsList().size() <= 0) {
				for (String friend : playerObject.getFriendsList()) {
					sendTargetAvatarStatus(player, friend);
				}
			}
		}

		final ChatFriendsListUpdate update = new ChatFriendsListUpdate(galaxy, firstName, online);
		playerManager.notifyPlayersWithCondition(new NotifyPlayersPacketIntent.ConditionalNotify() {
			@Override
			public boolean meetsCondition(Player player) {
				if (player.getPlayerState() != PlayerState.ZONED_IN)
					return false;

				PlayerObject playerObject = player.getPlayerObject();
				if (playerObject == null || playerObject.getFriendsList().size() <= 0)
					return false;

				List<String> friends = playerObject.getFriendsList();
				return friends.contains(update.getFriendName());
			}
		}, update);
	}

	private void sendTargetAvatarStatus(Player player, String target) {
		PlayerObject object = player.getPlayerObject();
		if (object == null)
			return;

		Player targetPlayer = player.getPlayerManager().getPlayerByCreatureFirstName(target);

		boolean online = true;
		if (targetPlayer == null || targetPlayer.getPlayerState() != PlayerState.ZONED_IN)
			online = false;

		ChatFriendsListUpdate update = new ChatFriendsListUpdate(player.getGalaxyName(), target, online);
		player.sendPacket(update);
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
			player.sendPacket(new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT, new OutOfBand(prose)));
		else
			player.sendPacket(new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT, message));
	}
	
	private void sendPersistentMessageHeaders(Player player, String galaxy) {
		if (player == null || player.getCreatureObject() == null)
			return;
		
		final List <Mail> playersMail = new LinkedList<>();
		final long receiverId = player.getCreatureObject().getObjectId();
		
		mails.traverse(new Traverser<Mail>() {
			@Override
			public void process(Mail element) {
				if (element.getReceiverId() == receiverId)
					playersMail.add(element);
			}
		});
		
		for (Mail mail : playersMail)
			sendPersistentMessage(player, mail, MailFlagType.HEADER_ONLY, galaxy);
	}
	
	private void sendPersistentMessage(Player receiver, Mail mail, MailFlagType requestType, String galaxy) {
		if (receiver == null || receiver.getCreatureObject() == null)
			return;
		
		ChatPersistentMessageToClient packet = null;
		
		switch (requestType) {
			case FULL_MESSAGE:
				packet = new ChatPersistentMessageToClient(false, mail.getSender(), galaxy, mail.getId(), mail.getSubject(), mail.getMessage(), mail.getTimestamp(), mail.getStatus()); 
				break;
			case HEADER_ONLY:
				packet = new ChatPersistentMessageToClient(true, mail.getSender(), galaxy, mail.getId(), mail.getSubject(), "", mail.getTimestamp(), mail.getStatus());
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
