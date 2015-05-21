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

import network.packets.swg.*;
import network.packets.swg.login.*;
import network.packets.swg.login.creation.*;
import network.packets.swg.zone.*;
import network.packets.swg.zone.auction.*;
import network.packets.swg.zone.baselines.*;
import network.packets.swg.zone.building.*;
import network.packets.swg.zone.chat.*;
import network.packets.swg.zone.combat.*;
import network.packets.swg.zone.deltas.*;
import network.packets.swg.zone.insertion.*;
import network.packets.swg.zone.object_controller.*;
import network.packets.swg.zone.server_ui.*;
import network.packets.swg.zone.spatial.*;

public enum PacketType {
	
	// Both
	SERVER_UNIX_EPOCH_TIME						(0x24b73893, ServerUnixEpochTime.class),
	SERVER_ID									(0x58c07f21, ServerId.class),
	SERVER_STRING								(0x0e20d7e9, ServerString.class),
	
	// Login
	CLIENT_ID_MSG								(0xd5899226, ClientIdMsg.class),
	ERROR_MESSAGE								(0xb5abf91a, ErrorMessage.class),
	ACCOUNT_FEATURE_BITS						(0x979f0279, AccountFeatureBits.class),
	CLIENT_PERMISSIONS_MESSAGE					(0xe00730e5, ClientPermissionsMessage.class),
	REQUEST_EXTENDED_CLUSTERS					(0x8e33ed05, RequestExtendedClusters.class),
	OFFLINE_SERVERS_MESSAGE     				(0xF41A5265, OfflineServersMessage.class),
		
		// Post-Login
		LOGIN_CLIENT_ID							(0x41131F96, LoginClientId.class),
		LOGIN_INCORRECT_CLIENT_ID				(0x20E7E510, LoginIncorrectClientId.class),
		LOGIN_CLIENT_TOKEN						(0xaab296c6, LoginClientToken.class),
		LOGIN_ENUM_CLUSTER						(0xc11c63b9, LoginEnumCluster.class),
		LOGIN_CLUSTER_STATUS					(0x3436aeb6, LoginClusterStatus.class),
		ENUMERATE_CHARACTER_ID					(0x65ea4574, EnumerateCharacterId.class),
		STATION_ID_HAS_JEDI_SLOT				(0xcc9fccf8, StationIdHasJediSlot.class),
		CHARACTER_CREATION_DISABLED				(0xf4a15265, CharacterCreationDisabled.class),
		
		// Character Creation
		CLIENT_CREATE_CHARACTER					(0xb97f3074, ClientCreateCharacter.class),
		CREATE_CHARACTER_SUCCESS				(0x1db575cc, CreateCharacterSuccess.class),
		CREATE_CHARACTER_FAILURE				(0xdf333c6e, CreateCharacterFailure.class),
		APPROVE_NAME_REQUEST					(0x9eb04b9f, ClientVerifyAndLockNameRequest.class),
		APPROVE_NAME_RESPONSE					(0x9b2c6ba7, ClientVerifyAndLockNameResponse.class),
		RANDOM_NAME_REQUEST						(0xd6d1b6d1, RandomNameRequest.class),
		RANDOM_NAME_RESPONSE					(0xe85fb868, RandomNameResponse.class),
		
		// Character Deletion
		DELETE_CHARACTER_RESPONSE				(0x8268989b, DeleteCharacterResponse.class),
		DELETE_CHARACTER_REQUEST				(0xe87ad031, DeleteCharacterRequest.class),
	
	// Zone
	COMMAND_QUEUE_ENQUEUE						(0x00000116, CommandQueueEnqueue.class),
	SELECT_CHARACTER							(0xb5098d76, SelectCharacter.class),
	CMD_SCENE_READY								(0x43fd1c22, CmdSceneReady.class),
	CMD_START_SCENE								(0x3ae6dfae, CmdStartScene.class),
	HEART_BEAT_MESSAGE							(0xa16cf9af, HeartBeatMessage.class),
	OBJECT_CONTROLLER							(0x80ce5e46, ObjectController.class),
	BASELINE									(0x68a75f0c, Baseline.class),
	DATA_TRANSFORM								(0x00000071, DataTransform.class),
	CONNECT_PLAYER_MESSAGE						(0x2e365218, ConnectPlayerMessage.class),
	CONNECT_PLAYER_RESPONSE_MESSAGE				(0x6137556F, ConnectPlayerResponseMessage.class),
	GALAXY_LOOP_TIMES_REQUEST					(0x7d842d68, GalaxyLoopTimesRequest.class),
	GALAXY_LOOP_TIMES_RESPONSE					(0x4e428088, GalaxyLoopTimesResponse.class),
	PARAMETERS_MESSAGE							(0x487652DA, ParametersMessage.class),
	DELTA										(0x12862153, DeltasMessage.class),
	SERVER_TIME_MESSAGE							(0x2EBC3BD9, ServerTimeMessage.class),
	SET_WAYPOINT_COLOR							(0x90C59FDE, SetWaypointColor.class),
	SHOW_BACKPACK									(ShowBackpack.CRC, ShowBackpack.class),
	SHOW_HELMET											(ShowHelmet.CRC, ShowHelmet.class),
	
		// Chat
		CHAT_FRIENDS_LIST_UPDATE				(0x6CD2FCD8, ChatFriendsListUpdate.class),
		CHAT_IGNORE_LIST						(0xF8C275B0, ChatIgnoreList.class),
		CHAT_INSTANT_MESSAGE_TO_CLIENT			(0x3C565CED, ChatInstantMessageToClient.class),
		CHAT_INSTANT_MESSAGE_TO_CHARACTER		(0x84BB21F7, ChatInstantMessageToCharacter.class),
		CHAT_ON_CONNECT_AVATAR					(0xD72FE9BE, ChatOnConnectAvatar.class),
		CHAT_ON_DESTROY_ROOM					(0xE8EC5877, ChatOnDestroyRoom.class),
		CHAT_ON_ENTERED_ROOM					(0xE69BDC0A, ChatOnEnteredRoom.class),
		CHAT_ON_LEAVE_ROOM						(0x60B5098B, ChatOnLeaveRoom.class),
		CHAT_ON_RECEIVE_ROOM_INVITATION			(0xC17EB06D, ChatOnReceiveRoomInvitation.class),
		CHAT_ON_SEND_INSTANT_MESSAGE			(0x88DBB381, ChatOnSendInstantMessage.class),
		CHAT_ON_SEND_ROOM_MESSAGE				(0xE7B61633, ChatOnSendRoomMessage.class),
		CHAT_PERSISTENT_MESSAGE_TO_CLIENT		(0x08485E17, ChatPersistentMessageToClient.class),
		CHAT_PERSISTENT_MESSAGE_TO_SERVER		(0x25A29FA6, ChatPersistentMessageToServer.class),
		CHAT_DELETE_PERSISTENT_MESSAGE			(0x8F251641, ChatDeletePersistentMessage.class),
		CHAT_REQUEST_PERSISTENT_MESSAGE			(0x07E3559F, ChatRequestPersistentMessage.class),
		CHAT_REQUEST_ROOM_LIST					(0x4C3d2CfA, ChatRequestRoomList.class),
		CHAT_ROOM_LIST							(0x70DEB197, ChatRoomList.class),
		CHAT_ROOM_MESSAGE						(0xCD4CE444, ChatRoomMessage.class),
		CHAT_SERVER_STATUS						(0x7102B15F, ChatServerStatus.class),
		CHAT_SYSTEM_MESSAGE						(0x6D2A6413, ChatSystemMessage.class),
		CON_GENERIC_MESSAGE						(0x08C5FC76, ConGenericMessage.class),
		VOICE_CHAT_STATUS						(0x9E601905, VoiceChatStatus.class),
		
		// Scene
		SCENE_END_BASELINES						(0x2C436037, SceneEndBaselines.class),
		SCENE_CREATE_OBJECT_BY_CRC				(0xFE89DDEA, SceneCreateObjectByCrc.class),
		SCENE_DESTROY_OBJECT					(0x4D45D504, SceneDestroyObject.class), 
		UPDATE_CONTAINMENT_MESSAGE				(0x56CBDE9E, UpdateContainmentMessage.class),
		UPDATE_CELL_PERMISSIONS_MESSAGE			(0xF612499C, UpdateCellPermissionMessage.class),
		GET_MAP_LOCATIONS_MESSAGE				(0x1A7AB839, GetMapLocationsMessage.class),
		GET_MAP_LOCATIONS_RESPONSE_MESSAGE		(0x9F80464C, GetMapLocationsResponseMessage.class),
		
		// Spatial
		UPDATE_POSTURE_MESSAGE					(0x0bde6b41, UpdatePostureMessage.class),
		UPDATE_TRANSFORMS_MESSAGE				(0x1B24F808, UpdateTransformsMessage.class),
		SPATIAL_CHAT							(0x000000f4, SpatialChat.class),
		NEW_TICKET_ACTIVITY_RESPONSE_MESSAGE	(0x6EA42D80, NewTicketActivityResponseMessage.class),
		ATTRIBUTE_LIST_MESSAGE					(0xF3F12F2A, AttributeListMessage.class),
		STOP_CLIENT_EFFECT_OBJECT_BY_LABEL		(0xAD6F6B26, StopClientEffectObjectByLabelMessage.class),
		OPENED_CONTAINER_MESSAGE				(0x2E11E4AB, OpenedContainerMessage.class),
		
		// Combat
		UPDATE_PVP_STATUS_MESSAGE				(0x08a1c126, UpdatePvpStatusMessage.class),
		GRANT_COMMAND_MESSAGE       			(0xE67E3875, GrantCommandMessage.class),
		
		// Server UI
		OBJECT_MENU_REQUEST						(0x00000146, ObjectMenuRequest.class),
		OBJECT_MENU_RESPONSE					(0x00000147, ObjectMenuResponse.class),
		SUI_CREATE_PAGE_MESSAGE					(0xD44B7259, SuiCreatePageMessage.class),
		SUI_EVENT_NOTIFICATION					(0x092D3564, SuiEventNotification.class),
		
		// Auction
		IS_VENDOR_OWNER_RESPONSE_MESSAGE		(0xCE04173E, IsVendorOwnerResponseMessage.class),
		AUCTION_QUERY_HEADERS_MESSAGE			(0x679E0D00, AuctionQueryHeadersMessage.class),
		GET_AUCTION_DETAILS						(0xD36EFAE4, GetAuctionDetails.class),
		GET_AUCTION_DETAILS_RESPONSE			(0xFE0E644B, GetAuctionDetailsResponse.class),
		CANCEL_LIVE_AUCTION_MESSAGE				(0x3687A4D2, CancelLiveAuctionMessage.class),
		CANCEL_LIVE_AUCTION_RESPONSE_MESSAGE	(0x7DA2246C, CancelLiveAuctionResponseMessage.class),
		AUCTION_QUERY_HEADERS_RESPONSE_MESSAGE	(0xFA500E52, AuctionQueryHeadersResponseMessage.class),
		RETRIEVE_AUCTION_ITEM_MESSAGE			(0x12B0D449, RetrieveAuctionItemMessage.class),
		RETRIEVE_AUCTION_ITEM_RESPONSE_MESSAGE	(0x9499EF8C, RetrieveAuctionItemResponseMessage.class),
	
	UNKNOWN (0xFFFFFFFF, SWGPacket.class);
	
	private static final Map <Integer, PacketType> packetMap = new HashMap<Integer, PacketType>();
	
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
	
	public static final PacketType fromCrc(int crc) {
		PacketType type = packetMap.get(crc);
		if (type == null)
			return UNKNOWN;
		return type;
	}
	
	public static final SWGPacket getForCrc(int crc) {
		PacketType type = packetMap.get(crc);
		if (type == null)
			return null;
		Class <? extends SWGPacket> c = type.c;
		try {
			return c.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
