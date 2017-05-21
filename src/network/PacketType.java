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
package network;

import java.util.HashMap;
import java.util.Map;

import com.projectswg.common.debug.Log;

import network.packets.swg.ErrorMessage;
import network.packets.swg.SWGPacket;
import network.packets.swg.ServerUnixEpochTime;
import network.packets.swg.admin.AdminShutdownServer;
import network.packets.swg.holo.HoloConnectionStarted;
import network.packets.swg.holo.HoloConnectionStopped;
import network.packets.swg.holo.HoloSetProtocolVersion;
import network.packets.swg.login.AccountFeatureBits;
import network.packets.swg.login.CharacterCreationDisabled;
import network.packets.swg.login.ClientIdMsg;
import network.packets.swg.login.ClientPermissionsMessage;
import network.packets.swg.login.ConnectionServerLagResponse;
import network.packets.swg.login.EnumerateCharacterId;
import network.packets.swg.login.LoginClientId;
import network.packets.swg.login.LoginClientToken;
import network.packets.swg.login.LoginClusterStatus;
import network.packets.swg.login.LoginEnumCluster;
import network.packets.swg.login.LoginIncorrectClientId;
import network.packets.swg.login.OfflineServersMessage;
import network.packets.swg.login.RequestExtendedClusters;
import network.packets.swg.login.ServerId;
import network.packets.swg.login.ServerString;
import network.packets.swg.login.StationIdHasJediSlot;
import network.packets.swg.login.creation.ClientCreateCharacter;
import network.packets.swg.login.creation.ClientVerifyAndLockNameRequest;
import network.packets.swg.login.creation.ClientVerifyAndLockNameResponse;
import network.packets.swg.login.creation.CreateCharacterFailure;
import network.packets.swg.login.creation.CreateCharacterSuccess;
import network.packets.swg.login.creation.DeleteCharacterRequest;
import network.packets.swg.login.creation.DeleteCharacterResponse;
import network.packets.swg.login.creation.RandomNameRequest;
import network.packets.swg.login.creation.RandomNameResponse;
import network.packets.swg.zone.ClientOpenContainerMessage;
import network.packets.swg.zone.CmdSceneReady;
import network.packets.swg.zone.ConnectPlayerResponseMessage;
import network.packets.swg.zone.EnterTicketPurchaseModeMessage;
import network.packets.swg.zone.ExpertiseRequestMessage;
import network.packets.swg.zone.GalaxyLoopTimesResponse;
import network.packets.swg.zone.GameServerLagResponse;
import network.packets.swg.zone.HeartBeat;
import network.packets.swg.zone.LagRequest;
import network.packets.swg.zone.ObjectMenuSelect;
import network.packets.swg.zone.ParametersMessage;
import network.packets.swg.zone.PlanetTravelPointListRequest;
import network.packets.swg.zone.PlanetTravelPointListResponse;
import network.packets.swg.zone.PlayClientEffectObjectMessage;
import network.packets.swg.zone.PlayMusicMessage;
import network.packets.swg.zone.RequestGalaxyLoopTimes;
import network.packets.swg.zone.SceneCreateObjectByCrc;
import network.packets.swg.zone.SceneDestroyObject;
import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.ServerNowEpochTime;
import network.packets.swg.zone.ServerTimeMessage;
import network.packets.swg.zone.ServerWeatherMessage;
import network.packets.swg.zone.SetWaypointColor;
import network.packets.swg.zone.ShowBackpack;
import network.packets.swg.zone.ShowHelmet;
import network.packets.swg.zone.StopClientEffectObjectByLabelMessage;
import network.packets.swg.zone.UpdateContainmentMessage;
import network.packets.swg.zone.UpdatePostureMessage;
import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.UpdateTransformMessage;
import network.packets.swg.zone.UpdateTransformWithParentMessage;
import network.packets.swg.zone.auction.AuctionQueryHeadersMessage;
import network.packets.swg.zone.auction.AuctionQueryHeadersResponseMessage;
import network.packets.swg.zone.auction.CancelLiveAuctionMessage;
import network.packets.swg.zone.auction.CancelLiveAuctionResponseMessage;
import network.packets.swg.zone.auction.GetAuctionDetails;
import network.packets.swg.zone.auction.GetAuctionDetailsResponse;
import network.packets.swg.zone.auction.IsVendorOwnerResponseMessage;
import network.packets.swg.zone.auction.RetrieveAuctionItemMessage;
import network.packets.swg.zone.auction.RetrieveAuctionItemResponseMessage;
import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.building.UpdateCellPermissionMessage;
import network.packets.swg.zone.chat.ChatAddModeratorToRoom;
import network.packets.swg.zone.chat.ChatBanAvatarFromRoom;
import network.packets.swg.zone.chat.ChatCreateRoom;
import network.packets.swg.zone.chat.ChatDeletePersistentMessage;
import network.packets.swg.zone.chat.ChatDestroyRoom;
import network.packets.swg.zone.chat.ChatEnterRoomById;
import network.packets.swg.zone.chat.ChatFriendsListUpdate;
import network.packets.swg.zone.chat.ChatIgnoreList;
import network.packets.swg.zone.chat.ChatInstantMessageToCharacter;
import network.packets.swg.zone.chat.ChatInstantMessageToClient;
import network.packets.swg.zone.chat.ChatInviteAvatarToRoom;
import network.packets.swg.zone.chat.ChatKickAvatarFromRoom;
import network.packets.swg.zone.chat.ChatOnConnectAvatar;
import network.packets.swg.zone.chat.ChatOnCreateRoom;
import network.packets.swg.zone.chat.ChatOnDestroyRoom;
import network.packets.swg.zone.chat.ChatOnEnteredRoom;
import network.packets.swg.zone.chat.ChatOnLeaveRoom;
import network.packets.swg.zone.chat.ChatOnReceiveRoomInvitation;
import network.packets.swg.zone.chat.ChatOnSendInstantMessage;
import network.packets.swg.zone.chat.ChatOnSendRoomMessage;
import network.packets.swg.zone.chat.ChatPersistentMessageToClient;
import network.packets.swg.zone.chat.ChatPersistentMessageToServer;
import network.packets.swg.zone.chat.ChatQueryRoom;
import network.packets.swg.zone.chat.ChatRemoveAvatarFromRoom;
import network.packets.swg.zone.chat.ChatRemoveModeratorFromRoom;
import network.packets.swg.zone.chat.ChatRequestPersistentMessage;
import network.packets.swg.zone.chat.ChatRequestRoomList;
import network.packets.swg.zone.chat.ChatRoomMessage;
import network.packets.swg.zone.chat.ChatSendToRoom;
import network.packets.swg.zone.chat.ChatSystemMessage;
import network.packets.swg.zone.chat.ChatUnbanAvatarFromRoom;
import network.packets.swg.zone.chat.ChatUninviteFromRoom;
import network.packets.swg.zone.chat.ConGenericMessage;
import network.packets.swg.zone.chat.VoiceChatStatus;
import network.packets.swg.zone.combat.GrantCommandMessage;
import network.packets.swg.zone.deltas.DeltasMessage;
import network.packets.swg.zone.insertion.ChatRoomList;
import network.packets.swg.zone.insertion.ChatServerStatus;
import network.packets.swg.zone.insertion.CmdStartScene;
import network.packets.swg.zone.insertion.ConnectPlayerMessage;
import network.packets.swg.zone.insertion.SelectCharacter;
import network.packets.swg.zone.object_controller.ChangeRoleIconChoice;
import network.packets.swg.zone.object_controller.CommandQueueEnqueue;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.DataTransformWithParent;
import network.packets.swg.zone.object_controller.ObjectController;
import network.packets.swg.zone.object_controller.ObjectMenuRequest;
import network.packets.swg.zone.object_controller.ObjectMenuResponse;
import network.packets.swg.zone.object_controller.ShowLootBox;
import network.packets.swg.zone.object_controller.SpatialChat;
import network.packets.swg.zone.server_ui.SuiCreatePageMessage;
import network.packets.swg.zone.server_ui.SuiEventNotification;
import network.packets.swg.zone.spatial.AttributeListMessage;
import network.packets.swg.zone.spatial.GetMapLocationsMessage;
import network.packets.swg.zone.spatial.GetMapLocationsResponseMessage;
import network.packets.swg.zone.spatial.NewTicketActivityResponseMessage;


public enum PacketType {

	// Holocore
	HOLO_SET_PROTOCOL_VERSION					(HoloSetProtocolVersion.CRC, HoloSetProtocolVersion.class),
	HOLO_CONNECTION_STARTED						(HoloConnectionStarted.CRC,	HoloConnectionStarted.class),
	HOLO_CONNECTION_STOPPED						(HoloConnectionStopped.CRC,	HoloConnectionStopped.class),
	
	// Admin
	ADMIN_SHUTDOWN_SERVER						(AdminShutdownServer.CRC,	AdminShutdownServer.class),
	
	// Both
	SERVER_UNIX_EPOCH_TIME						(ServerUnixEpochTime.CRC, 	ServerUnixEpochTime.class),
	SERVER_ID									(ServerId.CRC, 				ServerId.class),
	SERVER_STRING								(ServerString.CRC, 			ServerString.class),
	LAG_REQUEST									(LagRequest.CRC,			LagRequest.class),

	// Login
	CLIENT_ID_MSG								(ClientIdMsg.CRC, 				ClientIdMsg.class),
	ERROR_MESSAGE								(ErrorMessage.CRC, 				ErrorMessage.class),
	ACCOUNT_FEATURE_BITS						(AccountFeatureBits.CRC, 		AccountFeatureBits.class),
	CLIENT_PERMISSIONS_MESSAGE					(ClientPermissionsMessage.CRC, 	ClientPermissionsMessage.class),
	REQUEST_EXTENDED_CLUSTERS					(RequestExtendedClusters.CRC, 	RequestExtendedClusters.class),
	OFFLINE_SERVERS_MESSAGE     				(OfflineServersMessage.CRC, 	OfflineServersMessage.class),
	SERVER_NOW_EPOCH_TIME						(ServerNowEpochTime.CRC,		ServerNowEpochTime.class),
	GAME_SERVER_LAG_RESPONSE					(GameServerLagResponse.CRC,		GameServerLagResponse.class),

		// Post-Login
		LOGIN_CLIENT_ID							(LoginClientId.CRC, 			LoginClientId.class),
		LOGIN_INCORRECT_CLIENT_ID				(LoginIncorrectClientId.CRC, 	LoginIncorrectClientId.class),
		LOGIN_CLIENT_TOKEN						(LoginClientToken.CRC, 			LoginClientToken.class),
		LOGIN_ENUM_CLUSTER						(LoginEnumCluster.CRC, 			LoginEnumCluster.class),
		LOGIN_CLUSTER_STATUS					(LoginClusterStatus.CRC, 		LoginClusterStatus.class),
		ENUMERATE_CHARACTER_ID					(EnumerateCharacterId.CRC, 		EnumerateCharacterId.class),
		STATION_ID_HAS_JEDI_SLOT				(StationIdHasJediSlot.CRC, 		StationIdHasJediSlot.class),
		CHARACTER_CREATION_DISABLED				(CharacterCreationDisabled.CRC, CharacterCreationDisabled.class),

		// Character Creation
		CLIENT_CREATE_CHARACTER					(ClientCreateCharacter.CRC, 			ClientCreateCharacter.class),
		CREATE_CHARACTER_SUCCESS				(CreateCharacterSuccess.CRC, 			CreateCharacterSuccess.class),
		CREATE_CHARACTER_FAILURE				(CreateCharacterFailure.CRC, 			CreateCharacterFailure.class),
		APPROVE_NAME_REQUEST					(ClientVerifyAndLockNameRequest.CRC,	ClientVerifyAndLockNameRequest.class),
		APPROVE_NAME_RESPONSE					(ClientVerifyAndLockNameResponse.CRC, 	ClientVerifyAndLockNameResponse.class),
		RANDOM_NAME_REQUEST						(RandomNameRequest.CRC, 				RandomNameRequest.class),
		RANDOM_NAME_RESPONSE					(RandomNameResponse.CRC, 				RandomNameResponse.class),

		// Character Deletion
		DELETE_CHARACTER_RESPONSE				(DeleteCharacterResponse.CRC, 	DeleteCharacterResponse.class),
		DELETE_CHARACTER_REQUEST				(DeleteCharacterRequest.CRC, 	DeleteCharacterRequest.class),

	// Zone
	CONNECTION_SERVER_LAG_RESPONSE				(ConnectionServerLagResponse.CRC,	ConnectionServerLagResponse.class),
	COMMAND_QUEUE_ENQUEUE						(0x00000116, 						CommandQueueEnqueue.class),
	SELECT_CHARACTER							(SelectCharacter.CRC, 				SelectCharacter.class),
	CMD_SCENE_READY								(CmdSceneReady.CRC, 				CmdSceneReady.class),
	CMD_START_SCENE								(CmdStartScene.CRC, 				CmdStartScene.class),
	HEART_BEAT_MESSAGE							(HeartBeat.CRC, 					HeartBeat.class),
	OBJECT_CONTROLLER							(ObjectController.CRC, 				ObjectController.class),
	BASELINE									(Baseline.CRC, 						Baseline.class),
	DATA_TRANSFORM								(0x00000071, 						DataTransform.class),
	DATA_TRANSFORM_PARENT                       (0x000000F1, 						DataTransformWithParent.class),
	CONNECT_PLAYER_MESSAGE						(ConnectPlayerMessage.CRC, 			ConnectPlayerMessage.class),
	CONNECT_PLAYER_RESPONSE_MESSAGE				(ConnectPlayerResponseMessage.CRC, 	ConnectPlayerResponseMessage.class),
	GALAXY_LOOP_TIMES_REQUEST					(RequestGalaxyLoopTimes.CRC, 		RequestGalaxyLoopTimes.class),
	GALAXY_LOOP_TIMES_RESPONSE					(GalaxyLoopTimesResponse.CRC, 		GalaxyLoopTimesResponse.class),
	PARAMETERS_MESSAGE							(ParametersMessage.CRC, 			ParametersMessage.class),
	DELTA										(DeltasMessage.CRC, 				DeltasMessage.class),
	SERVER_TIME_MESSAGE							(ServerTimeMessage.CRC, 			ServerTimeMessage.class),
	SET_WAYPOINT_COLOR							(SetWaypointColor.CRC, 				SetWaypointColor.class),
	SHOW_BACKPACK								(ShowBackpack.CRC, 					ShowBackpack.class),
	SHOW_HELMET									(ShowHelmet.CRC, 					ShowHelmet.class),
	SERVER_WEATHER_MESSAGE						(ServerWeatherMessage.CRC, 			ServerWeatherMessage.class),
	PLAY_MUSIC_MESSAGE							(PlayMusicMessage.CRC,				PlayMusicMessage.class),
	PLAY_CLIENT_EFFECT_OBJECT_MESSAGE			(PlayClientEffectObjectMessage.CRC, PlayClientEffectObjectMessage.class),
	STOP_CLIENT_EFFECT_OBJECT_BY_LABEL			(StopClientEffectObjectByLabelMessage.CRC, 	StopClientEffectObjectByLabelMessage.class),
	EXPERTISE_REQUEST_MESSAGE					(ExpertiseRequestMessage.CRC,		ExpertiseRequestMessage.class),
	CHANGE_ROLE_ICON_CHOICE						(ChangeRoleIconChoice.CRC,			ChangeRoleIconChoice.class),
	SHOW_LOOT_BOX								(ShowLootBox.CRC,					ShowLootBox.class),
	
		// Chat
		CHAT_CREATE_ROOM						(ChatCreateRoom.CRC,				ChatCreateRoom.class),
		CHAT_DESTROY_ROOM						(ChatDestroyRoom.CRC,				ChatDestroyRoom.class),
		CHAT_ON_CREATE_ROOM						(ChatOnCreateRoom.CRC,				ChatOnCreateRoom.class),
		CHAT_FRIENDS_LIST_UPDATE				(ChatFriendsListUpdate.CRC, 		ChatFriendsListUpdate.class),
		CHAT_IGNORE_LIST						(ChatIgnoreList.CRC, 				ChatIgnoreList.class),
		CHAT_INSTANT_MESSAGE_TO_CLIENT			(ChatInstantMessageToClient.CRC, 	ChatInstantMessageToClient.class),
		CHAT_INSTANT_MESSAGE_TO_CHARACTER		(ChatInstantMessageToCharacter.CRC, ChatInstantMessageToCharacter.class),
		CHAT_ON_CONNECT_AVATAR					(ChatOnConnectAvatar.CRC,			ChatOnConnectAvatar.class),
		CHAT_ON_DESTROY_ROOM					(ChatOnDestroyRoom.CRC, 			ChatOnDestroyRoom.class),
		CHAT_ON_ENTERED_ROOM					(ChatOnEnteredRoom.CRC, 			ChatOnEnteredRoom.class),
		CHAT_ON_LEAVE_ROOM						(ChatOnLeaveRoom.CRC, 				ChatOnLeaveRoom.class),
		CHAT_ON_RECEIVE_ROOM_INVITATION			(ChatOnReceiveRoomInvitation.CRC, 	ChatOnReceiveRoomInvitation.class),
		CHAT_ON_SEND_INSTANT_MESSAGE			(ChatOnSendInstantMessage.CRC, 		ChatOnSendInstantMessage.class),
		CHAT_ON_SEND_ROOM_MESSAGE				(ChatOnSendRoomMessage.CRC, 		ChatOnSendRoomMessage.class),
		CHAT_PERSISTENT_MESSAGE_TO_CLIENT		(ChatPersistentMessageToClient.CRC, ChatPersistentMessageToClient.class),
		CHAT_PERSISTENT_MESSAGE_TO_SERVER		(ChatPersistentMessageToServer.CRC, ChatPersistentMessageToServer.class),
		CHAT_DELETE_PERSISTENT_MESSAGE			(ChatDeletePersistentMessage.CRC, 	ChatDeletePersistentMessage.class),
		CHAT_REQUEST_PERSISTENT_MESSAGE			(ChatRequestPersistentMessage.CRC, 	ChatRequestPersistentMessage.class),
		CHAT_REQUEST_ROOM_LIST					(ChatRequestRoomList.CRC, 			ChatRequestRoomList.class),
		CHAT_ENTER_ROOM_BY_ID					(ChatEnterRoomById.CRC, 			ChatEnterRoomById.class),
		CHAT_QUERY_ROOM							(ChatQueryRoom.CRC, 				ChatQueryRoom.class),
		CHAT_ROOM_LIST							(ChatRoomList.CRC, 					ChatRoomList.class),
		CHAT_ROOM_MESSAGE						(ChatRoomMessage.CRC,				ChatRoomMessage.class),
		CHAT_SEND_TO_ROOM						(ChatSendToRoom.CRC, 				ChatSendToRoom.class),
		CHAT_REMOVE_AVATAR_FROM_ROOM			(ChatRemoveAvatarFromRoom.CRC, 		ChatRemoveAvatarFromRoom.class),
		CHAT_SERVER_STATUS						(ChatServerStatus.CRC, 				ChatServerStatus.class),
		CHAT_SYSTEM_MESSAGE						(ChatSystemMessage.CRC, 			ChatSystemMessage.class),
		CHAT_INVITE_AVATAR_TO_ROOM				(ChatInviteAvatarToRoom.CRC,		ChatInviteAvatarToRoom.class),
		CHAT_UNINVITE_FROM_ROOM					(ChatUninviteFromRoom.CRC,			ChatUninviteFromRoom.class),
		CHAT_KICK_AVATAR_FROM_ROOM				(ChatKickAvatarFromRoom.CRC,		ChatKickAvatarFromRoom.class),
		CHAT_BAN_AVATAR_FROM_ROOM				(ChatBanAvatarFromRoom.CRC,			ChatBanAvatarFromRoom.class),
		CHAT_UNBAN_AVATAR_FROM_ROOM				(ChatUnbanAvatarFromRoom.CRC,		ChatUnbanAvatarFromRoom.class),
		CHAT_ADD_MODERATOR_TO_ROOM				(ChatAddModeratorToRoom.CRC,		ChatAddModeratorToRoom.class),
		CHAT_REMOVE_MODERATOR_FROM_ROOM			(ChatRemoveModeratorFromRoom.CRC,	ChatRemoveModeratorFromRoom.class),
		CON_GENERIC_MESSAGE						(ConGenericMessage.CRC, 			ConGenericMessage.class),
		VOICE_CHAT_STATUS						(VoiceChatStatus.CRC, 				VoiceChatStatus.class),

		// Scene
		SCENE_END_BASELINES						(SceneEndBaselines.CRC, 				SceneEndBaselines.class),
		SCENE_CREATE_OBJECT_BY_CRC				(SceneCreateObjectByCrc.CRC, 			SceneCreateObjectByCrc.class),
		SCENE_DESTROY_OBJECT					(SceneDestroyObject.CRC, 				SceneDestroyObject.class),
		UPDATE_CONTAINMENT_MESSAGE				(UpdateContainmentMessage.CRC, 			UpdateContainmentMessage.class),
		UPDATE_CELL_PERMISSIONS_MESSAGE			(UpdateCellPermissionMessage.CRC, 		UpdateCellPermissionMessage.class),
		GET_MAP_LOCATIONS_MESSAGE				(GetMapLocationsMessage.CRC, 			GetMapLocationsMessage.class),
		GET_MAP_LOCATIONS_RESPONSE_MESSAGE		(GetMapLocationsResponseMessage.CRC, 	GetMapLocationsResponseMessage.class),

		// Spatial
		UPDATE_POSTURE_MESSAGE					(UpdatePostureMessage.CRC, 					UpdatePostureMessage.class),
		UPDATE_TRANSFORMS_MESSAGE				(UpdateTransformMessage.CRC, 				UpdateTransformMessage.class),
		UPDATE_TRANSFORM_WITH_PARENT_MESSAGE    (UpdateTransformWithParentMessage.CRC, 		UpdateTransformWithParentMessage.class),
		SPATIAL_CHAT							(0x000000f4, 								SpatialChat.class),
		NEW_TICKET_ACTIVITY_RESPONSE_MESSAGE	(NewTicketActivityResponseMessage.CRC, 		NewTicketActivityResponseMessage.class),
		ATTRIBUTE_LIST_MESSAGE					(AttributeListMessage.CRC, 					AttributeListMessage.class),
		OPENED_CONTAINER_MESSAGE				(ClientOpenContainerMessage.CRC, 			ClientOpenContainerMessage.class),

		// Combat
		UPDATE_PVP_STATUS_MESSAGE				(UpdatePvpStatusMessage.CRC, 	UpdatePvpStatusMessage.class),
		GRANT_COMMAND_MESSAGE       			(GrantCommandMessage.CRC, 		GrantCommandMessage.class),

		// Server UI
		OBJECT_MENU_REQUEST						(0x00000146, 				ObjectMenuRequest.class),
		OBJECT_MENU_RESPONSE					(0x00000147, 				ObjectMenuResponse.class),
		OBJECT_MENU_SELECT						(ObjectMenuSelect.CRC,		ObjectMenuSelect.class),
		SUI_CREATE_PAGE_MESSAGE					(SuiCreatePageMessage.CRC, 	SuiCreatePageMessage.class),
		SUI_EVENT_NOTIFICATION					(SuiEventNotification.CRC, 	SuiEventNotification.class),

		// Auction
		IS_VENDOR_OWNER_RESPONSE_MESSAGE		(IsVendorOwnerResponseMessage.CRC, 			IsVendorOwnerResponseMessage.class),
		AUCTION_QUERY_HEADERS_MESSAGE			(AuctionQueryHeadersMessage.CRC, 			AuctionQueryHeadersMessage.class),
		GET_AUCTION_DETAILS						(GetAuctionDetails.CRC, 					GetAuctionDetails.class),
		GET_AUCTION_DETAILS_RESPONSE			(GetAuctionDetailsResponse.CRC, 			GetAuctionDetailsResponse.class),
		CANCEL_LIVE_AUCTION_MESSAGE				(CancelLiveAuctionMessage.CRC, 				CancelLiveAuctionMessage.class),
		CANCEL_LIVE_AUCTION_RESPONSE_MESSAGE	(CancelLiveAuctionResponseMessage.CRC, 		CancelLiveAuctionResponseMessage.class),
		AUCTION_QUERY_HEADERS_RESPONSE_MESSAGE	(AuctionQueryHeadersResponseMessage.CRC, 	AuctionQueryHeadersResponseMessage.class),
		RETRIEVE_AUCTION_ITEM_MESSAGE			(RetrieveAuctionItemMessage.CRC, 			RetrieveAuctionItemMessage.class),
		RETRIEVE_AUCTION_ITEM_RESPONSE_MESSAGE	(RetrieveAuctionItemResponseMessage.CRC, 	RetrieveAuctionItemResponseMessage.class),

		
		// Travel
		ENTER_TICKET_PURCHASE_MODE_MESSAGE		(EnterTicketPurchaseModeMessage.CRC,		EnterTicketPurchaseModeMessage.class),
		PLANET_TRAVEL_POINT_LIST_REQUEST		(PlanetTravelPointListRequest.CRC,			PlanetTravelPointListRequest.class),
		PLANET_TRAVEL_POINT_LIST_RESPONSE		(PlanetTravelPointListResponse.CRC,			PlanetTravelPointListResponse.class),
		
		
	UNKNOWN (0xFFFFFFFF, SWGPacket.class);

	private static final Map <Integer, PacketType> packetMap = new HashMap<>();

	static {
		for (PacketType type : values()) {
			packetMap.put(type.crc, type);
		}
	}

	private int crc;
	private Class <? extends SWGPacket> c;

	PacketType(int crc, Class <? extends SWGPacket> c) {
		this.crc = crc;
		this.c = c;
	}

	public int getCrc() {
		return crc;
	}
	
	public Class<? extends SWGPacket> getSwgClass() {
		return c;
	}

	public static PacketType fromCrc(int crc) {
		PacketType type = packetMap.get(crc);
		if (type == null)
			return UNKNOWN;
		return type;
	}

	public static SWGPacket getForCrc(int crc) {
		PacketType type = packetMap.get(crc);
		if (type == null)
			return null;
		Class <? extends SWGPacket> c = type.c;
		try {
			return c.newInstance();
		} catch (Exception e) {
			Log.e("Packet: [%08X] %s", crc, c.getName());
			Log.e(e);
		}
		return null;
	}

}
