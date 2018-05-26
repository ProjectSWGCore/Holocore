package com.projectswg.holocore.services.support.global.chat;

import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.holocore.intents.support.global.zone.NotifyPlayersPacketIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

public class ChatSystemService extends Service {
	
	public ChatSystemService() {
		
	}
	
	@IntentHandler
	private void handleSystemMessageIntent(SystemMessageIntent smi) {
		switch (smi.getBroadcastType()) {
			case AREA:
				broadcastAreaMessage(smi.getMessage(), smi.getReceiver());
				break;
			case PLANET:
				broadcastPlanetMessage(smi.getMessage(), smi.getTerrain());
				break;
			case GALAXY:
				broadcastGalaxyMessage(smi.getMessage());
				break;
			case PERSONAL:
				if (smi.getProse() != null) {
					broadcastPersonalMessage(smi.getReceiver(), smi.getProse());
				} else {
					broadcastPersonalMessage(smi.getReceiver(), smi.getMessage());
				}
				break;
		}
	}
	
	private void broadcastAreaMessage(String message, Player broadcaster) {
		broadcaster.getCreatureObject().sendObservers(new ChatSystemMessage(SystemChatType.PERSONAL, message));
	}
	
	private void broadcastPlanetMessage(String message, Terrain terrain) {
		ChatSystemMessage SWGPacket = new ChatSystemMessage(SystemChatType.PERSONAL, message);
		new NotifyPlayersPacketIntent(SWGPacket, terrain).broadcast();
	}
	
	private void broadcastGalaxyMessage(String message) {
		ChatSystemMessage SWGPacket = new ChatSystemMessage(SystemChatType.PERSONAL, message);
		new NotifyPlayersPacketIntent(SWGPacket).broadcast();
	}
	
	private void broadcastPersonalMessage(Player player, String message) {
		player.sendPacket(new ChatSystemMessage(SystemChatType.PERSONAL, message));
	}
	
	private void broadcastPersonalMessage(Player player, ProsePackage prose) {
		player.sendPacket(new ChatSystemMessage(SystemChatType.PERSONAL, new OutOfBandPackage(prose)));
	}
	
}
