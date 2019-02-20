/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.common.data.encodables.player.Mail;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.chat.*;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.chat.PersistentMessageIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatMailService extends Service {
	
	private AtomicInteger maxMailId;
	
	public ChatMailService() {
		this.maxMailId = new AtomicInteger(0);
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject obj = oci.getObject();
		if (!(obj instanceof PlayerObject))
			return;
		
		PlayerObject player = (PlayerObject) obj;
		int highestMailId = player.getMail().stream().mapToInt(Mail::getId).max().orElse(0);
		maxMailId.updateAndGet(prevMax -> prevMax >= highestMailId ? prevMax : highestMailId);
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		if (pei.getEvent() == PlayerEvent.PE_FIRST_ZONE) {
			sendPersistentMessageHeaders(pei.getPlayer());
		}
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket p = gpi.getPacket();
		switch (p.getPacketType()) {
			/* Mails */
			case CHAT_PERSISTENT_MESSAGE_TO_SERVER:
				if (p instanceof ChatPersistentMessageToServer)
					handleSendPersistentMessage(gpi.getPlayer(), (ChatPersistentMessageToServer) p);
				break;
			case CHAT_REQUEST_PERSISTENT_MESSAGE:
				if (p instanceof ChatRequestPersistentMessage)
					handlePersistentMessageRequest(gpi.getPlayer(), (ChatRequestPersistentMessage) p);
				break;
			case CHAT_DELETE_PERSISTENT_MESSAGE:
				if (p instanceof ChatDeletePersistentMessage)
					deletePersistentMessage(gpi.getPlayer(), ((ChatDeletePersistentMessage) p).getMailId());
				break;
			default:
				break;
		}
	}
	
	private void handleSendPersistentMessage(Player sender, ChatPersistentMessageToServer request) {
		String recipientStr = request.getRecipient().toLowerCase(Locale.ENGLISH);
		
		if (recipientStr.contains(" "))
			recipientStr = recipientStr.split(" ")[0];
		
		Player recipient = PlayerLookup.getPlayerByFirstName(recipientStr);
		CreatureObject recipientCreature = PlayerLookup.getCharacterByFirstName(recipientStr);
		ChatResult result = ChatResult.SUCCESS;
		
		if (recipientCreature == null)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		else if (recipientCreature.getPlayerObject() == null)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		
		if (sender.getPlayerObject().isIgnored(recipientStr))
			result = ChatResult.IGNORED;
		
		sender.sendPacket(new ChatOnSendPersistentMessage(result, request.getCounter()));
		
		if (result != ChatResult.SUCCESS)
			return;
		
		Mail mail = new Mail(sender.getCharacterName().split(" ")[0].toLowerCase(Locale.US), request.getSubject(), request.getMessage(), recipientCreature.getObjectId());
		mail.setId(maxMailId.incrementAndGet());
		mail.setTimestamp(Instant.now());
		mail.setOutOfBandPackage(request.getOutOfBandPackage());
		recipientCreature.getPlayerObject().addMail(mail);
		
		if (recipient != null) {
			sendPersistentMessage(recipient, mail, MailFlagType.HEADER_ONLY);
		}
	}
	
	@IntentHandler
	private void handlePersistentMessageIntent(PersistentMessageIntent pmi) {
		if (pmi.getReceiver() == null)
			return;
		
		Player recipient = pmi.getReceiver().getOwner();
		
		if (recipient == null)
			return;
		PlayerObject player = recipient.getPlayerObject();
		if (player == null)
			return;
		
		Mail mail = pmi.getMail();
		mail.setId(maxMailId.incrementAndGet());
		mail.setTimestamp(Instant.now());
		player.addMail(mail);
		
		sendPersistentMessage(recipient, mail, MailFlagType.HEADER_ONLY);
	}
	
	private void handlePersistentMessageRequest(Player player, ChatRequestPersistentMessage request) {
		if (player == null)
			return;
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;
		
		Mail mail = ghost.getMail(request.getMailId());
		if (mail == null)
			return;
		
		mail.setStatus(Mail.READ);
		sendPersistentMessage(player, mail, MailFlagType.FULL_MESSAGE);
	}
	
	private void sendPersistentMessageHeaders(Player player) {
		if (player == null || player.getCreatureObject() == null)
			return;
		
		for (Mail mail : player.getPlayerObject().getMail())
			sendPersistentMessage(player, mail, MailFlagType.HEADER_ONLY);
	}
	
	private void sendPersistentMessage(Player receiver, Mail mail, MailFlagType requestType) {
		if (receiver == null)
			return;
		PlayerObject ghost = receiver.getPlayerObject();
		if (ghost == null)
			return;
		
		if (ghost.isIgnored(mail.getSender())) {
			ghost.removeMail(mail);
			return;
		}
		
		ChatPersistentMessageToClient packet = null;
		
		switch (requestType) {
			case FULL_MESSAGE:
				packet = new ChatPersistentMessageToClient(mail, ProjectSWG.getGalaxy().getName(), false);
				break;
			case HEADER_ONLY:
				packet = new ChatPersistentMessageToClient(mail, ProjectSWG.getGalaxy().getName(), true);
				break;
		}
		
		receiver.sendPacket(packet);
	}
	
	private void deletePersistentMessage(Player player, int mailId) {
		if (player == null)
			return;
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;
		
		ghost.removeMail(mailId);
	}
	
	private enum MailFlagType {
		FULL_MESSAGE,
		HEADER_ONLY
	}
	
}
