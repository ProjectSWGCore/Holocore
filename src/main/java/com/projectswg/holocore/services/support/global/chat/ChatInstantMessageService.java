/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
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

import com.projectswg.common.data.encodables.chat.ChatResult;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.chat.ChatInstantMessageToCharacter;
import com.projectswg.common.network.packets.swg.zone.chat.ChatInstantMessageToClient;
import com.projectswg.common.network.packets.swg.zone.chat.ChatOnSendInstantMessage;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Locale;

public class ChatInstantMessageService extends Service {
	
	public ChatInstantMessageService() {
		
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		if (packet instanceof ChatInstantMessageToCharacter)
			handleInstantMessage(gpi.getPlayer(), (ChatInstantMessageToCharacter) packet);
	}
	
	private void handleInstantMessage(Player sender, ChatInstantMessageToCharacter request) {
		String strReceiver = request.getCharacter().toLowerCase(Locale.ENGLISH);
		String strSender = sender.getCharacterFirstName();
		
		Player receiver = PlayerLookup.getPlayerByFirstName(strReceiver);
		
		ChatResult result = ChatResult.SUCCESS;
		if (receiver == null || receiver.getPlayerState() != PlayerState.ZONED_IN)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		else if (receiver.getPlayerObject().isIgnored(strSender))
			result = ChatResult.IGNORED;
		
		sender.sendPacket(new ChatOnSendInstantMessage(result.getCode(), request.getSequence()));
		
		if (result != ChatResult.SUCCESS)
			return;
		
		receiver.sendPacket(new ChatInstantMessageToClient(request.getGalaxy(), strSender, request.getMessage()));
	}
	
}
