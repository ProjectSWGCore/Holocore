package com.projectswg.holocore.services.support.global.chat;

import com.projectswg.common.network.packets.swg.zone.chat.ChatOnEnteredRoom;
import com.projectswg.common.network.packets.swg.zone.insertion.ChatRoomList;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericPlayer;
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TestChatRoomService extends TestRunnerSynchronousIntents {
	
	private static long OBJECT_ID = 0;
	
	private GenericPlayer player1;
	private GenericPlayer player2;
	
	@Before
	public void setup() {
		registerService(new CharacterLookupService());
		registerService(new ChatRoomService());
		
		player1 = createPlayer("Player1");
		player2 = createPlayer("Player2");
	}
	
	@Test
	public void testSendChatRoomListOnFirstZone() {
		PlayerEventIntent intent = new PlayerEventIntent(player1, PlayerEvent.PE_FIRST_ZONE);
		broadcastAndWait(intent);
		
		ChatRoomList chatRoomList = player1.getNextPacket(ChatRoomList.class);
		assertNotNull("ChatRoomList packet should be sent when zoning", chatRoomList);
	}
	
	@Test
	public void testSendChatOnEnteredRoomOnFirstZone() {
		PlayerEventIntent intent = new PlayerEventIntent(player1, PlayerEvent.PE_FIRST_ZONE);
		broadcastAndWait(intent);
		
		ChatOnEnteredRoom chatOnEnteredRoom = player1.getNextPacket(ChatOnEnteredRoom.class);
		assertNotNull("ChatOnEnteredRoom packet should be sent when zoning for the first time", chatOnEnteredRoom);
	}
	
	@Test
	public void testSendChatOnEnteredRoomOnLaterZones() {
		PlayerEventIntent intent = new PlayerEventIntent(player1, PlayerEvent.PE_ZONE_IN_SERVER);
		broadcastAndWait(intent);
		
		ChatOnEnteredRoom chatOnEnteredRoom = player1.getNextPacket(ChatOnEnteredRoom.class);
		
		assertNull("ChatOnEnteredRoom packet should not be sent when zoning after the first time", chatOnEnteredRoom);
	}
	
	@Test
	public void testSendChatRoomListOnLaterZones() {
		PlayerEventIntent intent = new PlayerEventIntent(player1, PlayerEvent.PE_ZONE_IN_SERVER);
		broadcastAndWait(intent);
		
		ChatRoomList chatRoomList = player1.getNextPacket(ChatRoomList.class);
		
		assertNull("ChatRoomList packet should not be sent when zoning after the first time", chatRoomList);
	}
	
	@Test
	public void testAdminsAutoJoinServerLogChannel() {
		player1.setAccessLevel(AccessLevel.DEV);
		
		PlayerEventIntent intent = new PlayerEventIntent(player1, PlayerEvent.PE_FIRST_ZONE);
		broadcastAndWait(intent);
		
		List<String> joinedChannels = player1.getPlayerObject().getJoinedChannels();
		assertTrue("An admin should join the server log channel automatically", joinedChannels.contains(ChatRoomService.LOG_ROOM_PATH));
	}
	
	@Test
	public void testNotifyWhenMemberEnters() {
		PlayerEventIntent intent = new PlayerEventIntent(player1, PlayerEvent.PE_FIRST_ZONE);
		broadcastAndWait(intent);
		player1.getNextPacket(ChatOnEnteredRoom.class);	// Ignore the first packet of this type because that's ourselves that are joining
		
		PlayerEventIntent intent2 = new PlayerEventIntent(player2, PlayerEvent.PE_FIRST_ZONE);
		broadcastAndWait(intent2);
		ChatOnEnteredRoom chatOnEnteredRoomOther = player1.getNextPacket(ChatOnEnteredRoom.class);
		
		
		assertNotNull("Members in a chat room should be notified when a member joins the room", chatOnEnteredRoomOther);
	}
	
	private GenericPlayer createPlayer(String characterName) {
		GenericPlayer player = new GenericPlayer();
		CreatureObject creatureObject = new GenericCreatureObject(OBJECT_ID++, characterName);
		PlayerObject playerObject = creatureObject.getPlayerObject();
		playerObject.addJoinedChannel("SWG..lok");	// Important that this is a channel that actually exists
		player.setCreatureObject(creatureObject);
		ObjectCreatedIntent.broadcast(playerObject);
		ObjectCreatedIntent.broadcast(creatureObject);
		waitForIntents();
		
		return player;
	}
}
