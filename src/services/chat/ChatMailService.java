/************************************************************************************
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
package services.chat;

import intents.PlayerEventIntent;
import intents.chat.PersistentMessageIntent;
import intents.network.GalacticPacketIntent;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.chat.ChatDeletePersistentMessage;
import network.packets.swg.zone.chat.ChatOnSendPersistentMessage;
import network.packets.swg.zone.chat.ChatPersistentMessageToClient;
import network.packets.swg.zone.chat.ChatPersistentMessageToServer;
import network.packets.swg.zone.chat.ChatRequestPersistentMessage;
import resources.chat.ChatResult;
import resources.encodables.player.Mail;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.Log;
import resources.server_info.ObjectDatabase;
import services.CoreManager;
import services.chat.ChatManager.ChatRange;
import services.chat.ChatManager.ChatType;
import services.player.PlayerManager;

import com.projectswg.common.control.Service;
import com.projectswg.common.info.RelationalServerData;
import com.projectswg.common.info.RelationalServerFactory;

public class ChatMailService extends Service {
	
	private final ObjectDatabase<Mail> mails;
	private final RelationalServerData chatLogs;
	private final PreparedStatement insertChatLog;
	private int maxMailId;
	
	public ChatMailService() {
		mails = new CachedObjectDatabase<>("odb/mails.db", Mail::create, Mail::saveMail);
		chatLogs = RelationalServerFactory.getServerDatabase("chat/chat_log.db");
		insertChatLog = chatLogs.prepareStatement("INSERT INTO chat_log VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		maxMailId = 1;
		
		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
		registerForIntent(PersistentMessageIntent.class, pmi -> handlePersistentMessageIntent(pmi));
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
	}
	
	@Override
	public boolean initialize() {
		mails.load();
		mails.traverse(mail -> {
			if (mail.getId() >= maxMailId)
				maxMailId = mail.getId() + 1;
		});
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		mails.close();
		return super.terminate();
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player player = pei.getPlayer();
		if (player == null)
			return;

		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				sendPersistentMessageHeaders(player);
				break;
			default:
				break;
		}
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi){ 
		Packet p = gpi.getPacket();
		if (!(p instanceof SWGPacket))
			return;
		SWGPacket swg = (SWGPacket) p;
		String galaxyName = CoreManager.getGalaxy().getName();
		switch (swg.getPacketType()) {
			/* Mails */
			case CHAT_PERSISTENT_MESSAGE_TO_SERVER:
				if (p instanceof ChatPersistentMessageToServer)
					handleSendPersistentMessage(gpi.getPlayerManager(), gpi.getPlayer(), galaxyName, (ChatPersistentMessageToServer) p);
				break;
			case CHAT_REQUEST_PERSISTENT_MESSAGE:
				if (p instanceof ChatRequestPersistentMessage)
					handlePersistentMessageRequest(gpi.getPlayer(), galaxyName, (ChatRequestPersistentMessage) p);
				break;
			case CHAT_DELETE_PERSISTENT_MESSAGE:
				if (p instanceof ChatDeletePersistentMessage)
					deletePersistentMessage(((ChatDeletePersistentMessage) p).getMailId());
				break;
			default: break;
		}
	}
	
	private void handleSendPersistentMessage(PlayerManager playerMgr, Player sender, String galaxy, ChatPersistentMessageToServer request) {
		String recipientStr = request.getRecipient().toLowerCase(Locale.ENGLISH);
		
		if (recipientStr.contains(" "))
			recipientStr = recipientStr.split(" ")[0];
		
		Player recipient = playerMgr.getPlayerByCreatureFirstName(recipientStr);
		long recId = (recipient == null ? playerMgr.getCharacterIdByName(request.getRecipient()) : recipient.getCreatureObject().getObjectId());
		ChatResult result = ChatResult.SUCCESS;
		
		if (recId == 0)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;

		if (sender.getPlayerObject().isIgnored(recipientStr))
			result = ChatResult.IGNORED;

		sender.sendPacket(new ChatOnSendPersistentMessage(result, request.getCounter()));

		if (result != ChatResult.SUCCESS)
			return;

		Mail mail = new Mail(sender.getCharacterName().split(" ")[0].toLowerCase(Locale.US), request.getSubject(), request.getMessage(), recId);
		mail.setId(maxMailId++);
		mail.setTimestamp((int) (new Date().getTime() / 1000));
		mail.setOutOfBandPackage(request.getOutOfBandPackage());
		mails.add(mail);
		
		if (recipient != null) {
			sendPersistentMessage(recipient, mail, MailFlagType.HEADER_ONLY, galaxy);
			SWGObject sendObj = sender.getCreatureObject();
			SWGObject recvObj = recipient.getCreatureObject();
			logChat(sendObj.getObjectId(), sendObj.getObjectName(), recvObj.getObjectId(), recvObj.getObjectName(), mail.getSubject(), mail.getMessage());
		}
	}
	
	private void handlePersistentMessageIntent(PersistentMessageIntent pmi) {
		if (pmi.getReceiver() == null)
			return;
		
		Player recipient = pmi.getReceiver().getOwner();
		
		if (recipient == null)
			return;
		
		Mail mail = pmi.getMail();
		mail.setId(maxMailId);
		maxMailId++;
		mail.setTimestamp((int) (new Date().getTime() / 1000));
		
		mails.add(mail);
		
		sendPersistentMessage(recipient, mail, MailFlagType.HEADER_ONLY, pmi.getGalaxy());
	}
	
	private void handlePersistentMessageRequest(Player player, String galaxy, ChatRequestPersistentMessage request) {
		Mail mail = getMail(request.getMailId());
		if (mail == null)
			return;
		
		if (mail.getReceiverId() != player.getCreatureObject().getObjectId())
			return;
		
		mail.setStatus(Mail.READ);
		sendPersistentMessage(player, mail, MailFlagType.FULL_MESSAGE, galaxy);
	}
	
	private Mail getMail(int id) {
		AtomicReference<Mail> ref = new AtomicReference<>(null);
		mails.traverseInterruptable((m) -> {
			if (m.getId() == id) {
				ref.set(m);
				return false;
			}
			return true;
		});
		return ref.get();
	}
	
	private void sendPersistentMessageHeaders(Player player) {
		if (player == null || player.getCreatureObject() == null)
			return;
		
		final List <Mail> playersMail = new LinkedList<>();
		final long receiverId = player.getCreatureObject().getObjectId();

		mails.traverse(element -> {
			if (element.getReceiverId() == receiverId)
				playersMail.add(element);
		});
		
		String galaxy = CoreManager.getGalaxy().getName();
		for (Mail mail : playersMail)
			sendPersistentMessage(player, mail, MailFlagType.HEADER_ONLY, galaxy);
	}
	
	private void sendPersistentMessage(Player receiver, Mail mail, MailFlagType requestType, String galaxy) {
		if (receiver == null || receiver.getCreatureObject() == null)
			return;

		PlayerObject ghost = receiver.getPlayerObject();
		if (ghost.isIgnored(mail.getSender())) {
			mails.remove(mail);
			return;
		}

		ChatPersistentMessageToClient packet = null;
		
		switch (requestType) {
			case FULL_MESSAGE:
				packet = new ChatPersistentMessageToClient(mail, galaxy, false);
				break;
			case HEADER_ONLY:
				packet = new ChatPersistentMessageToClient(mail, galaxy, true);
				break;
		}
		
		receiver.sendPacket(packet);
	}
	
	private void deletePersistentMessage(int mailId) {
		mails.remove(getMail(mailId));
	}
	
	private void logChat(long sendId, String sendName, long recvId, String recvName, String subject, String message) {
		try {
			synchronized (insertChatLog) {
				insertChatLog.setLong(1, System.currentTimeMillis());
				insertChatLog.setLong(2, sendId);
				insertChatLog.setString(3, sendName);
				insertChatLog.setLong(4, recvId);
				insertChatLog.setString(5, recvName);
				insertChatLog.setString(6, ChatType.MAIL.name());
				insertChatLog.setString(7, ChatRange.PERSONAL.name());
				insertChatLog.setString(8, "");
				insertChatLog.setString(9, subject);
				insertChatLog.setString(10, message);
				insertChatLog.executeUpdate();
			}
		} catch (SQLException e) {
			Log.e(e);
		}
	}

	private enum MailFlagType {
		FULL_MESSAGE,
		HEADER_ONLY
	}
	
}
