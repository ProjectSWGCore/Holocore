package com.projectswg.holocore.services.support.global.chat;

import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.zone.NotifyPlayersPacketIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

public class ChatSystemService extends Service {
	
	public ChatSystemService() {
		
	}
	
	@IntentHandler
	private void handleSystemMessageIntent(SystemMessageIntent smi) {
		SystemChatType systemChatType = smi.getSystemChatType();
		
		switch (smi.getBroadcastType()) {
			case AREA:
				broadcastAreaMessage(smi.getMessage(), smi.getReceiver(), systemChatType);
				break;
			case PLANET:
				broadcastPlanetMessage(smi.getMessage(), smi.getTerrain(), systemChatType);
				break;
			case GALAXY:
				broadcastGalaxyMessage(smi.getMessage(), systemChatType);
				break;
			case PERSONAL:
				if (smi.getProse() != null) {
					broadcastPersonalMessage(smi.getReceiver(), smi.getProse(), systemChatType);
				} else {
					broadcastPersonalMessage(smi.getReceiver(), smi.getMessage(), systemChatType);
				}
				break;
		}
	}
	
	private void broadcastAreaMessage(String message, Player broadcaster, SystemChatType systemChatType) {
		broadcaster.getCreatureObject().sendObservers(new ChatSystemMessage(systemChatType, message));
	}
	
	private void broadcastPlanetMessage(String message, Terrain terrain, SystemChatType systemChatType) {
		ChatSystemMessage SWGPacket = new ChatSystemMessage(systemChatType, message);
		new NotifyPlayersPacketIntent(SWGPacket, terrain).broadcast();
	}
	
	private void broadcastGalaxyMessage(String message, SystemChatType systemChatType) {
		ChatSystemMessage SWGPacket = new ChatSystemMessage(systemChatType, message);
		new NotifyPlayersPacketIntent(SWGPacket).broadcast();
	}
	
	private void broadcastPersonalMessage(Player player, String message, SystemChatType systemChatType) {
		player.sendPacket(new ChatSystemMessage(systemChatType, message));
	}
	
	private void broadcastPersonalMessage(Player player, ProsePackage prose, SystemChatType systemChatType) {
		player.sendPacket(new ChatSystemMessage(systemChatType, new OutOfBandPackage(prose)));
	}
	
}
