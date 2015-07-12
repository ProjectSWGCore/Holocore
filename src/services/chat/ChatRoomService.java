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
import intents.chat.ChatRoomUpdateIntent;
import intents.network.GalacticPacketIntent;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.chat.ChatCreateRoom;
import network.packets.swg.zone.chat.ChatDestroyRoom;
import network.packets.swg.zone.chat.ChatEnterRoomById;
import network.packets.swg.zone.chat.ChatOnCreateRoom;
import network.packets.swg.zone.chat.ChatOnDestroyRoom;
import network.packets.swg.zone.chat.ChatOnEnteredRoom;
import network.packets.swg.zone.chat.ChatOnLeaveRoom;
import network.packets.swg.zone.chat.ChatOnSendRoomMessage;
import network.packets.swg.zone.chat.ChatQueryRoom;
import network.packets.swg.zone.chat.ChatQueryRoomResults;
import network.packets.swg.zone.chat.ChatRemoveAvatarFromRoom;
import network.packets.swg.zone.chat.ChatRequestRoomList;
import network.packets.swg.zone.chat.ChatSendToRoom;
import network.packets.swg.zone.insertion.ChatRoomList;
import resources.Terrain;
import resources.chat.ChatAvatar;
import resources.chat.ChatResult;
import resources.chat.ChatRoom;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.player.PlayerObject;
import resources.player.AccessLevel;
import resources.player.Player;
import services.player.PlayerManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Waverunner
 */
public class ChatRoomService extends Service {
	// Array of only the valid terrains in relation to zones able to use planetary-based chat
	private static final List<Terrain> terrains = Arrays.asList(
			Terrain.CORELLIA, Terrain.DANTOOINE, Terrain.DATHOMIR, Terrain.ENDOR, Terrain.KASHYYYK, Terrain.LOK,
			Terrain.MUSTAFAR, Terrain.NABOO, Terrain.RORI, Terrain.TATOOINE, Terrain.TALUS, Terrain.YAVIN4
	);
	private int maxChatRoomId;
	private Map<Integer, ChatRoom> roomMap;
	// Map to keep track of each player's recent message for a room to prevent duplicates from client
	private Map<Long, Map<Integer, Integer>> messages;

	public ChatRoomService() {
		roomMap 	= new ConcurrentHashMap<>();
		messages	= new ConcurrentHashMap<>();
		maxChatRoomId = 1;
	}

	@Override
	public boolean initialize() {
		// TODO: Load up persistent channels
		registerForIntent(ChatRoomUpdateIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);

		return super.initialize();
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case ChatRoomUpdateIntent.TYPE:
				processChatRoomUpdateIntent((ChatRoomUpdateIntent) i);
				break;
			case GalacticPacketIntent.TYPE:
				processPacket((GalacticPacketIntent) i);
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
				if (p instanceof ChatRemoveAvatarFromRoom)
					leaveChatChannel(player, ((ChatRemoveAvatarFromRoom) p).getPath());
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
			default: break;
		}
	}

	private void processChatRoomUpdateIntent(ChatRoomUpdateIntent i) {
		switch(i.getUpdateType()) {
			case CREATE: createRoom(i.getAvatar(), i.isPublic(), i.getPath(), i.getTitle()); break;
			case DESTROY: notifyDestroyRoom(i.getAvatar(), i.getPath(), 0); break;
			default: break;
		}
	}

	/* Chat Rooms */

	private void handleChatDestroyRoom(Player player, ChatDestroyRoom p) {
		ChatRoom room = roomMap.get(p.getRoomId());
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		if ((room == null || !room.getCreator().equals(avatar) || !room.getOwner().equals(avatar))) {
			player.sendPacket(new ChatOnDestroyRoom(ChatAvatar.getFromPlayer(player), ChatResult.ROOM_AVATAR_NO_PERMISSION.getCode(), p.getRoomId(), p.getSequence()));
			return;
		}

		if (!notifyDestroyRoom(avatar, room.getPath(), p.getSequence())) {
			player.sendPacket(new ChatOnDestroyRoom(ChatAvatar.getFromPlayer(player), ChatResult.NONE.getCode(), p.getRoomId(), p.getSequence()));
		}
	}

	private void handleChatCreateRoom(Player player, ChatCreateRoom p) {
		String path = p.getRoomName();
		String title = p.getRoomTitle();
		ChatRoom room = getRoom(path);

		ChatResult result = ChatResult.SUCCESS;
		if (room != null)
			result = ChatResult.ROOM_ALREADY_EXISTS;

		if (result == ChatResult.SUCCESS) {
			room = createRoom(ChatAvatar.getFromPlayer(player), p.isPublic(), path, title);
			room.setMuted(p.isModerated());
		}

		player.sendPacket(new ChatOnCreateRoom(result.getCode(), room, p.getSequence()));
	}

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

	public void enterChatChannels(Player player, List<String> channels) {
		for (String s : channels) {
			enterChatChannel(player, s);
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

		room.getMembers().add(avatar);
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
		// Channel was not found, attempt to remove it from this players list of channels if it exists.
		// This can happen if a channel was deleted while the player was offline
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null) {
			System.err.println("Tried to join a room with a path that does not exist: " + path);
			return;
		}
		ghost.removeJoinedChannel(path);
	}

	public void leaveChatChannel(Player player, ChatRoom room, int sequence) {
		ChatAvatar avatar = ChatAvatar.getFromPlayer(player);

		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return; // ChatOnLeaveRoom doesn't do anything other than for a ChatResult.SUCCESS, so no need to send a fail

		if (!room.getMembers().remove(avatar) && !ghost.removeJoinedChannel(room.getPath()))
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

		if (!destroyer.equals(ChatAvatar.getSystemAvatar(destroyer.getGalaxy()))) {
			new NotifyPlayersPacketIntent(new ChatOnDestroyRoom(destroyer, ChatResult.SUCCESS.getCode(), room.getId(), sequence),
					Collections.singletonList(destroyer.getNetworkId())).broadcast();
		}

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
		// TODO: Move to server datatable file (SDF): Room (s) | Title (s)
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
		createRoom(systemAvatar, true, basePath + "GuildLeader", "Guild leader chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Mayor", "Mayor chat for this galaxy");
		createRoom(systemAvatar, true, basePath + "Trader", "Trader chat for this galaxy");
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
		// TODO: Move planets to a server datatable file (SDF): Planet (s)
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

}
