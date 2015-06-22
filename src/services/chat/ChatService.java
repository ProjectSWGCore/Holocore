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
import intents.chat.ChatRoomUpdateIntent;
import intents.chat.PersistentMessageIntent;
import intents.chat.SpatialChatIntent;
import intents.network.GalacticPacketIntent;
import intents.player.ZonePlayerSwapIntent;
import intents.server.ServerStatusIntent;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.chat.*;
import network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import network.packets.swg.zone.insertion.ChatRoomList;
import network.packets.swg.zone.object_controller.SpatialChat;
import resources.Galaxy;
import resources.Terrain;
import resources.chat.ChatAvatar;
import resources.chat.ChatResult;
import resources.chat.ChatRoom;
import resources.collections.SWGList;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.OutOfBandPackage;
import resources.encodables.ProsePackage;
import resources.encodables.player.Mail;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.player.PlayerState;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;
import services.player.PlayerManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatService extends Service {

	// Array of only the valid terrains in relation to zones able to use planetary-based chat
	private static final List<Terrain> terrains = Arrays.asList(
			Terrain.CORELLIA, Terrain.DANTOOINE, Terrain.DATHOMIR, Terrain.ENDOR, Terrain.KASHYYYK, Terrain.LOK,
			Terrain.MUSTAFAR, Terrain.NABOO, Terrain.RORI, Terrain.TATOOINE, Terrain.TALUS, Terrain.YAVIN4
	);
	private ObjectDatabase<Mail> mails;
	private int maxMailId;
	private int maxChatRoomId;
	private Map<Integer, ChatRoom> roomMap;
	// Map to keep track of each player's recent message for a room to prevent duplicates from client
	private Map<Long, Map<Integer, Integer>> messages;

	public ChatService() {
		roomMap = new HashMap<>();
		mails = new CachedObjectDatabase<>("odb/mails.db");
		maxMailId = 1;
		maxChatRoomId = 1;
		messages = new ConcurrentHashMap<>();
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
		registerForIntent(ChatRoomUpdateIntent.TYPE);
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
			case ChatRoomUpdateIntent.TYPE:
				if (i instanceof ChatRoomUpdateIntent)
					handleChatRoomUpdateIntent((ChatRoomUpdateIntent) i);
				break;
		}
	}

	private void handleChatRoomUpdateIntent(ChatRoomUpdateIntent i) {
		switch(i.getUpdateType()) {
			case CREATE: createRoom(i.getAvatar(), i.isPublic(), i.getPath(), i.getTitle());
			default: break;
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
			case CHAT_QUERY_ROOM:
				if (p instanceof ChatQueryRoom)
					handleChatQueryRoom(player, (ChatQueryRoom) p);
				break;
			case CHAT_ENTER_ROOM_BY_ID:
				if (p instanceof ChatEnterRoomById) {
					ChatEnterRoomById enterRoomById = (ChatEnterRoomById) p;
					enterChatChannel(player, enterRoomById.getRoomId(), enterRoomById.getSequence());
				}
				break;
			case CHAT_REMOVE_AVATAR_FROM_ROOM:
				if (p instanceof ChatRemoveAvatarFromRoom)
					leaveChatChannel(player, ((ChatRemoveAvatarFromRoom) p).getPath());
				break;
			case CHAT_SEND_TO_ROOM:
				if (p instanceof ChatSendToRoom)
					handleChatSendToRoom(player, (ChatSendToRoom) p);
				break;
			case CHAT_REQUEST_ROOM_LIST:
				if (p instanceof ChatRequestRoomList)
					handleChatRoomListRequest(player);
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
		Player player = intent.getPlayer();
		if (player == null)
			return;

		switch (intent.getEvent()) {
			case PE_ZONE_IN:
				switchPlanetaryChatChannels(player);
				break;
			case PE_FIRST_ZONE:
				sendPersistentMessageHeaders(player, intent.getGalaxy());
				updateChatAvatarStatus(player, intent.getGalaxy(), true);
				enterJoinedChatChannels(player);
				break;
			case PE_LOGGED_OUT:
				if (player.getCreatureObject() == null)
					return;
				updateChatAvatarStatus(player, intent.getGalaxy(), false);
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

	/* Chat Rooms */

	private void handleChatSendToRoom(Player player, ChatSendToRoom p) {
		ChatResult result = ChatResult.SUCCESS;

		ChatRoom room = getRoom(p.getRoomId());
		if (room == null)
			result = ChatResult.ROOM_INVALID_ID;

		if (result != ChatResult.SUCCESS) {
			player.sendPacket(new ChatOnSendRoomMessage(result.getCode(), p.getSequence()));
			return;
		}

		if (!incrementMessageCounter(player.getNetworkId(), room.getId(), p.getSequence()))
			return;

		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		result = room.canSendMessage(avatar);

		// TODO: Check length of messages -- Result 16 is used for too long message

		player.sendPacket(new ChatOnSendRoomMessage(result.getCode(), p.getSequence()));

		if (result == ChatResult.SUCCESS)
			room.sendMessage(avatar, p.getMessage(), p.getOutOfBandPackage(), player.getPlayerManager());
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
			if (!chatRoom.isPublic() && !chatRoom.getInvited().contains(avatar) && !chatRoom.getOwner().equals(avatar))
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

	private void enterJoinedChatChannels(Player player) {
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;

		for (String s : ghost.getJoinedChannels()) {
			enterChatChannel(player, s);
		}
	}

	private void switchPlanetaryChatChannels(Player player) {
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

		Terrain terrain = player.getCreatureObject().getLocation().getTerrain();

		// Enter the new zone-only chat channels
		if (!terrains.contains(terrain))
			return;

		String planetPath = "SWG." + player.getGalaxyName() + "." + terrain.getNameCapitalized() + ".";
		enterChatChannel(player, planetPath + "Planet");
		enterChatChannel(player, planetPath + "system");
	}

	/**
	 * Attempts to join the specified chat channel
	 * @param player Player joining the chat channel
	 * @param room Chat room to enter
	 */
	public void enterChatChannel(Player player, ChatRoom room, int sequence) {
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null) {
			sendOnEnteredChatRoom(player, avatar, ChatResult.NONE, room.getId(), sequence);
			return;
		}

		ChatResult result = room.canJoinRoom(avatar);
		if (result != ChatResult.SUCCESS && player.getAccessLevel() != AccessLevel.PLAYER) {
			sendOnEnteredChatRoom(player, avatar, result, room.getId(), sequence);
			return;
		}
		// TODO: Check if player is appropriate faction for the room (Rebel and imperial chat rooms)

		// Server-based list so we can join chat channels automatically
		List<String> joinedChannels = ghost.getJoinedChannels();
		if (!joinedChannels.contains(room.getPath()))
			joinedChannels.add(room.getPath());

		room.getMembers().add(avatar);

		// Re-send the player the room list with just this room as it could have been private/hidden
		// This also "refreshes" the client, not sending this will cause a Chat channel unavailable message.
		ChatRoomList roomList = new ChatRoomList(room);
		player.sendPacket(roomList);

		// Notify players of success, it's ChatResult.SUCCESS at this point
		player.sendPacket(new ChatOnEnteredRoom(avatar, result.getCode(), room.getId(), sequence));

		PlayerManager manager = player.getPlayerManager();
		// Notify everyone that a player entered the room
		room.sendPacketToMembers(manager, new ChatOnEnteredRoom(avatar, result.getCode(), room.getId(), 0));
	}

	public void enterChatChannel(Player player, int id, int sequence) {
		ChatRoom room = getRoom(id);
		if (room == null) {
			sendOnEnteredChatRoom(player, ChatAvatar.getFromPlayer(player), ChatResult.NONE, id, sequence);
			return;
		}
		enterChatChannel(player, room, sequence);
	}

	public void enterChatChannel(Player player, String path) {
		for (ChatRoom room : roomMap.values()) {
			if (room.getPath().equals(path)) {
				enterChatChannel(player, room, 0);
				return;
			}
		}
		System.err.println("Tried to join a room with a path that does not exist: " + path);
	}

	public void leaveChatChannel(Player player, ChatRoom room, int sequence) {
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return; // ChatOnLeaveRoom doesn't do anything other than for a ChatResult.SUCCESS, so no need to send a fail

		if (!room.getMembers().remove(avatar) && !ghost.getJoinedChannels().remove(room.getPath()))
			return;

		player.sendPacket(new ChatOnLeaveRoom(avatar, ChatResult.SUCCESS.getCode(), room.getId(), sequence));

		room.sendPacketToMembers(player.getPlayerManager(), new ChatOnLeaveRoom(avatar, ChatResult.SUCCESS.getCode(), room.getId(), 0));
	}

	public void leaveChatChannel(Player player, String path) {
		for (ChatRoom chatRoom : roomMap.values()) {
			if (chatRoom.getPath().equals(path))
				leaveChatChannel(player, chatRoom, 0);
		}
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
		if (path.isEmpty() || path.endsWith("."))
			return null;

		String base = "SWG." + creator.getGalaxy();
		if (!path.startsWith(base) || path.equals(base))
			return null;

		// All paths should have parents, lets validate to make sure they exist first. Create them if they don't.
		int lastIndex = path.lastIndexOf(".");
		if (lastIndex != -1) {
			String parentPath = path.substring(0, lastIndex);
			if (getRoom(parentPath) == null) {
				createRoom(creator, isPublic, parentPath, "");
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

		roomMap.put(id, room);
		return room;
	}

	private void createSystemChannels(String galaxy) {
		ChatAvatar systemAvatar = ChatAvatar.getSystemAvatar(galaxy);
		// TODO: Move these to a Server Data File?
		String basePath = "SWG." + galaxy + ".";
		createRoom(systemAvatar, true, basePath + "Galaxy", "public chat for the whole galaxy, cannot create rooms here");
		createRoom(systemAvatar, true, basePath + "system", "system messages for this galaxy");
		createRoom(systemAvatar, true, basePath + "Chat",   "public chat for this galaxy, can create rooms here");
		createRoom(systemAvatar, true, basePath + "Imperial", "Imperial chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "ImperialWarRoom", "Imperial war room chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Rebel",   "Rebel chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "RebelWarRoom", "Rebel war room chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "BountyHunter", "Bounty Hunter chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Commando", "Commando chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Entertainer", "Entertainer chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "ForceSensitive", "Force Sensitive chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Medic", "Medic chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Officer", "Officer chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Pilot", "Pilot chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Politician", "Politician chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Smuggler", "Smuggler chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Spy", "Spy chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Trader", "Trader chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "BeastMastery", "Beast Mastery chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Auction", "Auction chat for this galaxy");
		createPlanetChannels(systemAvatar, basePath);
	}

	private void createPlanetChannels(ChatAvatar systemAvatar, String basePath) {
		for (Terrain terrain : terrains) {
			String path = basePath + terrain.getNameCapitalized() + ".";
			createRoom(systemAvatar, true, path + "Planet", "public chat for this planet, cannot create rooms here");
			createRoom(systemAvatar, true, path + "system", "system messages for this planet, cannot create rooms here");
			createRoom(systemAvatar, true, path + "Chat", "public chat for this planet, can create rooms here");
		}
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

	public void initializeGalaxyChannels(Galaxy galaxy) {
		createSystemChannels(galaxy.getName());
		// TODO: Load up persistent channels
		/** Channel Notes
		 * Group channels: created by System
		 * 	- SWG.serverName.group.GroupObjectId.GroupChat 	title = GroupId
		 * Guild channels: created by System
		 * 	- SWG.serverName.guild.GuildId.GuildChat 		title = GuildId
		 * City channels: created by System
		 * 	- SWG.serverName.city.CityId.CityChat			title = CityId
		 */
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

	/* Friends List */

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

	private void handleSpatialChat(SpatialChatIntent i) {
		Player sender = i.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), 0, i.getMessage(), (short) i.getChatType(), (short) i.getMoodId());
		sender.sendPacket(message);
		
		// Notify observers of the chat message
		for (SWGObject aware : actor.getObservers()) {
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

		sender.sendPacket(new ChatOnSendPersistentMessage(result, request.getCounter()));

		if (result != ChatResult.SUCCESS)
			return;

		Mail mail = new Mail(sender.getCharacterName(), request.getSubject(), request.getMessage(), recId);
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
			player.sendPacket(new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT, new OutOfBandPackage(prose)));
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
