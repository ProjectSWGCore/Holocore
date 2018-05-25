package com.projectswg.holocore.services.support.global.chat;

import com.projectswg.common.network.packets.swg.zone.object_controller.SpatialChat;
import com.projectswg.holocore.intents.support.global.chat.SpatialChatIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

public class ChatSpatialService extends Service {
	
	public ChatSpatialService() {
		
	}
	
	@IntentHandler
	private void handleSpatialChatIntent(SpatialChatIntent spi) {
		Player sender = spi.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		String senderName = sender.getCharacterFirstName();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), 0, spi.getMessage(), (short) spi.getChatType(), (short) spi.getMoodId());
		
		// Notify observers of the chat message
		for (Player owner : actor.getObservers()) {
			if (owner.getPlayerObject().isIgnored(senderName))
				continue;
			owner.sendPacket(new SpatialChat(owner.getCreatureObject().getObjectId(), message));
		}
	}
	
}
