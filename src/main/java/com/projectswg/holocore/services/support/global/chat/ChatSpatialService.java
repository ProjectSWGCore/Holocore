/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
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
		this.chatRange = PswgDatabase.INSTANCE.getConfig().getInt(this, "spatialChatRange", 128);
	}
	
	@IntentHandler
	private void handleSpatialChatIntent(SpatialChatIntent spi) {
		Player sender = spi.getPlayer();
		SWGObject actor = sender.getCreatureObject();
		String senderName = sender.getCharacterFirstName();
		
		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), actor.getObjectId(), spi.getTargetId(), spi.getMessage(), (short) spi.getChatType(), (short) spi.getMoodId(), (byte) spi.getLanguageId());
		
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
