package com.projectswg.holocore.services.support.global.chat;

import com.projectswg.common.network.packets.swg.zone.object_controller.SpatialChat;
import com.projectswg.holocore.intents.support.global.chat.SpatialChatIntent;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

public class ChatSpatialService extends Service {
	
	private final int chatRange;
	
	public ChatSpatialService() {
		this.chatRange = PswgDatabase.config().getInt(this, "spatialChatRange", 128);
	}
	
	@IntentHandler
	private void handleSpatialChatIntent(SpatialChatIntent spi) {
		Player sender = spi.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		String senderName = sender.getCharacterFirstName();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), 0, spi.getMessage(), (short) spi.getChatType(), (short) 0);
		
		// Notify observers of the chat message
		for (Player owner : actor.getObservers()) {
			if (owner.getPlayerObject().isIgnored(senderName))
				continue;
			CreatureObject creature = owner.getCreatureObject();
			if (creature.getWorldLocation().distanceTo(actor.getWorldLocation()) > chatRange)
				continue;
			owner.sendPacket(new SpatialChat(creature.getObjectId(), message));
		}
	}
	
}
