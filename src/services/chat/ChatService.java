package services.chat;

import java.util.List;

import intents.GalacticPacketIntent;
import intents.chat.SpatialChatIntent;
import network.packets.Packet;
import network.packets.swg.zone.ChatRequestRoomList;
import network.packets.swg.zone.object_controller.ObjectController;
import network.packets.swg.zone.object_controller.SpatialChat;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.player.Player;

public class ChatService extends Service {
	
	public ChatService() {
		
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(SpatialChatIntent.TYPE);
		return super.initialize();
	}
	
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			Packet p = ((GalacticPacketIntent) i).getPacket();
			long netId = ((GalacticPacketIntent) i).getNetworkId();
			Player player = ((GalacticPacketIntent) i).getPlayerManager().getPlayerFromNetworkId(netId);
			if (player != null) {
				if (p instanceof ChatRequestRoomList)
					handleChatRoomListRequest(player, (ChatRequestRoomList) p);
			}
		} 
		else if (i instanceof SpatialChatIntent)
			handleSpatialChat((SpatialChatIntent) i);

	}
	
	private void handleChatRoomListRequest(Player player, ChatRequestRoomList request) {
//		ChatRoomList list = new ChatRoomList();
//		list.addChatRoom(new ChatRoom(1, 0, 0, "SWG.Josh Wifi.Chat.tcpa", "SWG", "Josh Wifi", player.getCreatureObject().getName(), player.getCreatureObject().getName(), "Chat"));
//		sendPacket(player, list);
	}
	
	private void handleSpatialChat(SpatialChatIntent i) {
		Player sender = i.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		String chatMsg = i.getMessage();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), 0, chatMsg, 0);
		ObjectController controller = new ObjectController(244, actor.getObjectId(), message);

		sender.sendPacket(controller);
		
		// Notify observers of the chat message
		List<Player> observers = actor.getObservers();
		for (Player observer : observers) {
			if (observer.getCreatureObject() == null)
				continue;
			
			message = new SpatialChat(actor.getObjectId(), 0, chatMsg, 0);
			controller = new ObjectController(244, observer.getCreatureObject().getObjectId(), message);
			observer.sendPacket(controller);
		}
	}
}
