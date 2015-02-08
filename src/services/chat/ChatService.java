package services.chat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import intents.GalacticPacketIntent;
import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import intents.ServerStatusIntent;
import intents.ServerStatusIntent.ServerStatus;
import intents.chat.ChatBroadcastIntent;
import intents.chat.ChatBroadcastIntent.BroadcastType;
import intents.chat.PersistentMessageIntent;
import intents.chat.SpatialChatIntent;
import network.packets.Packet;
import network.packets.swg.zone.ChatRequestRoomList;
import network.packets.swg.zone.chat.ChatDeletePersistentMessage;
import network.packets.swg.zone.chat.ChatInstantMessageToCharacter;
import network.packets.swg.zone.chat.ChatInstantMessageToClient;
import network.packets.swg.zone.chat.ChatOnSendInstantMessage;
import network.packets.swg.zone.chat.ChatOnSendPersistentMessage;
import network.packets.swg.zone.chat.ChatPersistentMessageToClient;
import network.packets.swg.zone.chat.ChatPersistentMessageToServer;
import network.packets.swg.zone.chat.ChatRequestPersistentMessage;
import network.packets.swg.zone.chat.ChatSystemMessage;
import network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import network.packets.swg.zone.object_controller.ObjectController;
import network.packets.swg.zone.object_controller.SpatialChat;
import resources.Terrain;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.player.Mail;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;
import services.player.PlayerManager;

public class ChatService extends Service {
	
	private ObjectDatabase<Mail> mails;
	private int maxMailId;
	
	public ChatService() {
		mails = new CachedObjectDatabase<Mail>("odb/mails.db");
		maxMailId = 1;
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(SpatialChatIntent.TYPE);
		registerForIntent(PersistentMessageIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ChatBroadcastIntent.TYPE);
		registerForIntent(ServerStatusIntent.TYPE);
		mails.load();
		mails.traverse(new Traverser<Mail>() {
			@Override
			public void process(Mail mail) {
				if (mail.getId() >= maxMailId)
					maxMailId = mail.getId() + 1;
			}
		});
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
				else if (p instanceof ChatInstantMessageToCharacter)
					handleInstantMessage(((GalacticPacketIntent) i).getPlayerManager(), player, (ChatInstantMessageToCharacter) p);
				else if (p instanceof ChatPersistentMessageToServer)
					handleSendPersistentMessage(((GalacticPacketIntent) i).getPlayerManager(), player, 
							((GalacticPacketIntent) i).getGalaxy().getName(), (ChatPersistentMessageToServer) p);
				else if (p instanceof ChatRequestPersistentMessage)
					handlePersistentMessageRequest(player, ((GalacticPacketIntent) i).getGalaxy().getName(), (ChatRequestPersistentMessage) p);
				else if (p instanceof ChatDeletePersistentMessage)
					deletePersistentMessage(((ChatDeletePersistentMessage) p).getMailId());
			}
		} 
		else if (i instanceof SpatialChatIntent)
			handleSpatialChat((SpatialChatIntent) i);
		else if (i instanceof PersistentMessageIntent)
			handlePersistentMessageIntent((PersistentMessageIntent) i);
		else if (i instanceof PlayerEventIntent)
			handlePlayerEventIntent((PlayerEventIntent) i);
		else if (i instanceof ChatBroadcastIntent)
			handleChatBroadcast((ChatBroadcastIntent) i);
		else if (i instanceof ServerStatusIntent && ((ServerStatusIntent) i).getStatus() == ServerStatus.SHUTDOWN_REQUESTED)
			sendShutdownBroadcasts(((ServerStatusIntent) i).getTime());
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		switch(intent.getEvent()) {
		case PE_ZONE_IN:
			sendPersistentMessageHeaders(intent.getPlayer(), intent.getGalaxy());
			break;
		default: break;
		}
	}
	
	private void handleChatBroadcast(ChatBroadcastIntent i) {
		if (i.getBroadcastType() == BroadcastType.AREA)
			broadcastAreaMessage(i.getMessage(), i.getBroadcaster());
		else broadcastGalaxyMessage(i.getMessage(), i.getTerrain());
	}

	private void handleChatRoomListRequest(Player player, ChatRequestRoomList request) {
//		ChatRoomList list = new ChatRoomList();
//		list.addChatRoom(new ChatRoom(1, 0, 0, "SWG.Josh Wifi.Chat.tcpa", "SWG", "Josh Wifi", player.getCreatureObject().getName(), player.getCreatureObject().getName(), "Chat"));
//		sendPacket(player, list);
	}
	
	private void handleSpatialChat(SpatialChatIntent i) {
		// TODO: Emotes, also figure out one of the unknown ints. Might be that "Player A says to Player B" is one of the unknowns. (targetId in SpatialChat packet)
		Player sender = i.getPlayer();
		SWGObject actor = sender.getCreatureObject();

		// Send to self
		SpatialChat message = new SpatialChat(actor.getObjectId(), 0, i.getMessage(), i.getChatType(), i.getMoodId());
		ObjectController controller = new ObjectController(SpatialChat.CRC, actor.getObjectId(), message);

		sender.sendPacket(controller);
		
		// Notify observers of the chat message
		for (Player observer : actor.getObservers()) {
			if (observer.getCreatureObject() == null)
				continue;
			
			controller = new ObjectController(244, observer.getCreatureObject().getObjectId(), message);
			observer.sendPacket(controller);
		}
	}
	
	private void handleInstantMessage(PlayerManager playerMgr, Player sender, ChatInstantMessageToCharacter request) {
		String strReceiver = request.getCharacter().toLowerCase();
		String strSender = sender.getCharacterName();
		
		if (strSender.contains(" "))
			strSender = strSender.split(" ")[0];
		
		Player receiver = playerMgr.getPlayerByCreatureFirstName(strReceiver);

		int errorCode = 0; // 0 = No issue, 4 = "strReceiver is not online"
		if (receiver == null)
			errorCode = 4;
		
		ChatOnSendInstantMessage response = new ChatOnSendInstantMessage(errorCode, request.getSequence());
		sender.sendPacket(response);
		
		if (errorCode == 4)
			return;
		
		receiver.sendPacket(new ChatInstantMessageToClient(request.getGalaxy(), strSender, request.getMessage()));
	}
	
	private void handleSendPersistentMessage(PlayerManager playerMgr, Player sender, String galaxy, ChatPersistentMessageToServer request) {

		String recipientStr = request.getRecipient().toLowerCase();
		
		if (recipientStr.contains(" "))
			recipientStr = recipientStr.split(" ")[0];
		
		Player recipient = playerMgr.getPlayerByCreatureFirstName(recipientStr);

		long recId = (recipient == null ? playerMgr.getCharacterIdByName(request.getRecipient()) : recipient.getCreatureObject().getObjectId());

		int errorCode = 0;
		
		if (recId == 0)
			errorCode = 4;
		
		ChatOnSendPersistentMessage response = new ChatOnSendPersistentMessage(errorCode, request.getCounter());
		sender.sendPacket(response);
		
		Mail mail = new Mail(sender.getCharacterName(), request.getSubject(), request.getMessage(), recId);
		mail.setId(maxMailId);
		maxMailId++;
		mail.setTimestamp((int) (new Date().getTime() / 1000));
		
		mails.put(mail.getId(), mail);
		
		if (recipient != null)
			sendPersistentMessage(recipient, mail, MailFlagType.HEADER_ONLY, galaxy);
	}
	
	private void handlePersistentMessageIntent(PersistentMessageIntent intent) {
		if (intent.getReceiver() == null)
			return;
		
		Player recipient = intent.getReceiver().getOwner();
		
		if (recipient == null)
			return;
		
		Mail mail = intent.getMail();
		mail.setId(maxMailId);
		maxMailId++;
		mail.setTimestamp((int) (new Date().getTime() / 1000));
		
		mails.put(mail.getId(), mail);
		
		sendPersistentMessage(recipient, mail, MailFlagType.HEADER_ONLY, intent.getGalaxy());
	}
	
	private void handlePersistentMessageRequest(Player player, String galaxy, ChatRequestPersistentMessage request) {
		Mail mail = mails.get(request.getMailId());
		
		if (mail == null)
			return;
		
		if (mail.getReceiverId() != player.getCreatureObject().getObjectId())
			return;
		
		sendPersistentMessage(player, mail, MailFlagType.FULL_MESSAGE, galaxy);
	}
	
	private void broadcastAreaMessage(String message, SWGObject broadcaster) {
		ChatSystemMessage packet = new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT.ordinal(), message);
		broadcaster.getOwner().sendPacket(packet);
		
		List<Player> observers = broadcaster.getObservers();
		for (Player player : observers) {
			player.sendPacket(packet);
		}
	}
	
	private void broadcastGalaxyMessage(String message, Terrain terrain) {
		ChatSystemMessage packet = new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT.ordinal(), message);
		new NotifyPlayersPacketIntent(packet, terrain).broadcast();
	}
	
	private void sendShutdownBroadcasts(final long time) {
		final AtomicInteger runCount = new AtomicInteger();
		
		final int interval = (int) (time / 3);

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
			public void run() {
				switch(runCount.getAndIncrement()) {
				case 0:
					broadcastGalaxyMessage(String.format("The server will be shutting down in %d minutes.", time), null);
					break;
				case 1:
					broadcastGalaxyMessage(String.format("The server will be shutting down in %d minutes.", time - interval), null);
					break;
				case 2:
					broadcastGalaxyMessage(String.format("The server will be shutting down in %d minutes.", time - (interval*2)), null);
					break;
				case 3:
					broadcastGalaxyMessage("The server will now be shutting down.", null);
					throw new RuntimeException("Reached max runCount"); // no "clean" ways to cancel the runnable I can think of
				}
			}
		}, 0, interval, TimeUnit.MINUTES);
		
	}
	
	private void sendPersistentMessageHeaders(Player player, String galaxy) {
		
		if (player == null || player.getCreatureObject() == null)
			return;
		
		final long receiverId = player.getCreatureObject().getObjectId();
		
		final List<Mail> playersMail = new ArrayList<Mail>();
		
		mails.traverse(new Traverser<Mail>() {
			@Override
			public void process(Mail element) {
				if (element.getReceiverId() == receiverId)
					playersMail.add(element);
			}
		});
		
		if (playersMail.isEmpty())
			return;
		
		for (Mail mail : playersMail) { sendPersistentMessage(player, mail, MailFlagType.HEADER_ONLY, galaxy); }
	}
	
	private void sendPersistentMessage(Player receiver, Mail mail, MailFlagType requestType, String galaxy) {
		if (receiver == null || receiver.getCreatureObject() == null)
			return;
		
		ChatPersistentMessageToClient packet = null;
		
		switch(requestType) {
		case FULL_MESSAGE:
			packet = new ChatPersistentMessageToClient((byte) 0, mail.getSender(), galaxy, mail.getId(), mail.getSubject(), mail.getMessage(), mail.getTimestamp(), mail.getStatus()); 
			break;

		case HEADER_ONLY:
			packet = new ChatPersistentMessageToClient((byte) 1, mail.getSender(), galaxy, mail.getId(), mail.getSubject(), "", mail.getTimestamp(), mail.getStatus());
			break;
			
		default: 
			packet = new ChatPersistentMessageToClient((byte) 1, mail.getSender(), galaxy, mail.getId(), mail.getSubject(), "", mail.getTimestamp(), mail.getStatus());
			break;
		}
		
		receiver.sendPacket(packet);
	}
	
	private void deletePersistentMessage(int mailId) {
		mails.remove(mailId);
	}
	
	private enum MailFlagType {
		FULL_MESSAGE,
		HEADER_ONLY;
	}
}
