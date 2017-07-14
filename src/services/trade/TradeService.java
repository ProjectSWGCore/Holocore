package services.trade;

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.control.Service;
import com.projectswg.common.debug.Log;

import intents.PlayerEventIntent;
import intents.chat.ChatBroadcastIntent;
import intents.network.GalacticPacketIntent;
import network.packets.Packet;
import network.packets.swg.zone.object_controller.SecureTrade;
import network.packets.swg.zone.object_controller.SecureTrade.TradeMessageType;
import network.packets.swg.zone.trade.AbortTradeMessage;
import network.packets.swg.zone.trade.AcceptTransactionMessage;
import network.packets.swg.zone.trade.AddItemMessage;
import network.packets.swg.zone.trade.BeginTradeMessage;
import network.packets.swg.zone.trade.BeginVerificationMessage;
import network.packets.swg.zone.trade.DenyTradeMessage;
import network.packets.swg.zone.trade.GiveMoneyMessage;
import network.packets.swg.zone.trade.RemoveItemMessage;
import network.packets.swg.zone.trade.TradeCompleteMessage;
import network.packets.swg.zone.trade.UnAcceptTransactionMessage;
import network.packets.swg.zone.trade.VerifyTradeMessage;
import resources.Posture;
import resources.containers.ContainerPermissionsType;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.sui.SuiButtons;
import resources.sui.SuiMessageBox;
import services.objects.ObjectManager;

public class TradeService extends Service {
	
	private final List<TradeSession> tradeSessions; 
	
	public TradeService() {
		tradeSessions = new ArrayList<TradeSession>();
		
		registerForIntent(GalacticPacketIntent.class,	this::handleGalacticPacketIntent); 
		registerForIntent(PlayerEventIntent.class,		this::handlePlayerEventIntent);
	}
	
	@Override
	public boolean stop() { 
		for (TradeSession tradeSession : tradeSessions) {
			tradeSession.sendAbortTrade();
		}
		return super.stop();
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		if (pei.getPlayer().getCreatureObject() == null)
			return;
		TradeSession session = pei.getPlayer().getCreatureObject().getTradeSession();
		if (session == null)
			return;
		
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
			case PE_LOGGED_OUT:
				session.sendAbortTrade();
				break;
			default:
				break;
		}
	}

	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();

		if (packet instanceof SecureTrade) {
			handleSecureTrade((SecureTrade) packet,gpi.getPlayer(), gpi.getObjectManager());
		} else if (packet instanceof AbortTradeMessage){
			handleAbortTradeMessage(gpi.getPlayer());
		} else if (packet instanceof DenyTradeMessage){
			handleDenyTradeMessage(gpi.getPlayer());
		} else if (packet instanceof AcceptTransactionMessage){
			handleAcceptTransactionMessage(gpi.getPlayer());
		} else if (packet instanceof UnAcceptTransactionMessage){
			handleUnAcceptTransactionMessage(gpi.getPlayer());
		} else if (packet instanceof AddItemMessage){
			handleAddItemMessage((AddItemMessage) packet, gpi.getPlayer(), gpi.getObjectManager());
		} else if (packet instanceof GiveMoneyMessage){
			handleGiveMoneyMessage((GiveMoneyMessage) packet, gpi.getPlayer());
		} else if (packet instanceof BeginVerificationMessage){
			handleBeginVerificationMessage(gpi.getPlayer());
		} else if (packet instanceof VerifyTradeMessage){
			handleVerifyTradeMessage(gpi.getPlayer(), gpi.getObjectManager());
		} else if (packet instanceof TradeCompleteMessage){
			handleTradeCompleteMessage(gpi.getPlayer());
		}
	}

	private void handleSecureTrade(SecureTrade packet, Player player, ObjectManager objectManager) {
		CreatureObject initiator = player.getCreatureObject();
		SWGObject accepterObject = objectManager.getObjectById(packet.getAccepterId());
		CreatureObject accepter;
		if (!(accepterObject instanceof CreatureObject) || !((CreatureObject) accepterObject).isPlayer()) { 
			sendSystemMessage(initiator.getOwner(), "start_fail_target_not_player");
			return;
		}
		accepter = (CreatureObject) accepterObject;
		
		if(initiator.isInCombat() || accepter.isInCombat()){ 
			return;
		}
		
		if(initiator.getPosture() == Posture.INCAPACITATED || accepter.getPosture() == Posture.INCAPACITATED){
			sendSystemMessage(initiator.getOwner(), "player_incapacitated");
			return;
		}
		
		if(initiator.getPosture() == Posture.DEAD || accepter.getPosture() == Posture.DEAD){
			sendSystemMessage(initiator.getOwner(), "player_dead");
			return;
		}
		
		TradeSession tradeSession = new TradeSession(initiator, accepter);
		tradeSessions.add(tradeSession);
		initiator.setTradeSession(tradeSession);
		handleTradeSessionRequest(packet, player, initiator, accepter);
		Log.d("Trade Session Request. Type=%s  Initiator=%s  Receipient=%s PacketSenderID: %d", packet.getType(), initiator, accepter, player.getCreatureObject().getObjectId());
	}

	private void handleAbortTradeMessage(Player player) {
		TradeSession tradeSession = player.getCreatureObject().getTradeSession();
		
		if(tradeSession == null)
			return;
		
		tradeSession.sendAbortTrade();
	}
	
	private void handleDenyTradeMessage(Player player) {
		TradeSession tradeSession = player.getCreatureObject().getTradeSession();
		
		if(tradeSession == null)
			return;
		
		tradeSession.sendToPartner(player.getCreatureObject(), new DenyTradeMessage());
	}

	private void handleAcceptTransactionMessage(Player player) {
		TradeSession tradeSession = player.getCreatureObject().getTradeSession();
		
		if(tradeSession == null)
			return;
		
		tradeSession.sendToPartner(player.getCreatureObject(), new AcceptTransactionMessage());
	}
	
	private void handleUnAcceptTransactionMessage(Player player) {
		TradeSession tradeSession = player.getCreatureObject().getTradeSession();
		
		if(tradeSession == null)
			return;
		
		tradeSession.sendToPartner(player.getCreatureObject(), new UnAcceptTransactionMessage());
	}
	
	private void handleAddItemMessage(AddItemMessage packet, Player player, ObjectManager objectManager) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		if (tradeSession == null) {
			Log.w("Invalid TradeSession for handleAddItemMessage. Creature %s: ", creature);
			return;
		}
		
		SWGObject tradeObject = objectManager.getObjectById(packet.getObjectId());
		if(creature.hasSlottedObject(tradeObject)){
			return;
		}			
		
		if(tradeObject.hasAttribute("no_trade")){
			sendSystemMessage(player, "add_item_failed_prose");
			tradeSession.sendToPartner(creature, new RemoveItemMessage(packet.getObjectId()));
			tradeSession.removeFromItemList(creature, packet.getObjectId());
		}
		
		tradeObject.setContainerPermissions(ContainerPermissionsType.INVENTORY);
		tradeSession.addItem(creature, tradeObject);
		tradeSession.sendToPartner(creature, new AddItemMessage(packet.getObjectId()));
		tradeSession.getTradePartner(creature).addCustomAware(tradeObject);
	}
	
	private void handleGiveMoneyMessage(GiveMoneyMessage packet, Player player) {
		TradeSession tradeSession = player.getCreatureObject().getTradeSession();
		tradeSession.setMoneyAmount(player.getCreatureObject(), packet.getMoneyAmount());
		tradeSession.sendToPartner(player.getCreatureObject(), new GiveMoneyMessage(packet.getMoneyAmount()));
	}
	
	private void handleVerifyTradeMessage(Player player, ObjectManager objectManager) {
		TradeSession tradeSession = player.getCreatureObject().getTradeSession();
		CreatureObject creature = player.getCreatureObject();
		CreatureObject initiator = tradeSession.getInitiator();
		CreatureObject accepter = tradeSession.getAccepter();
		
		if(creature.equals(accepter) && !tradeSession.isAccepterVerified()){
			accepter.sendSelf(new VerifyTradeMessage());
			tradeSession.setAccepterVerified(true);
		}
		
		if(creature.equals(initiator) && !tradeSession.isInitiatorVerified()){
			initiator.sendSelf(new VerifyTradeMessage());
			tradeSession.setInititatorVerified(true);
		}
		
		if (!tradeSession.isInitiatorVerified() || !tradeSession.isAccepterVerified())
			return;
	
		tradeSession.moveToPartnerInventory(accepter, tradeSession.getFromItemList(accepter));
		tradeSession.moveToPartnerInventory(initiator, tradeSession.getFromItemList(initiator));
		
		long initiatorTransfer = tradeSession.getMoneyAmount(initiator);
		long accepterTransfer = tradeSession.getMoneyAmount(accepter);
		
		initiator.setCashBalance(initiator.getCashBalance() + accepterTransfer - initiatorTransfer);
		accepter.setCashBalance(accepter.getCashBalance() + initiatorTransfer - accepterTransfer);
		
		accepter.sendSelf(new TradeCompleteMessage());
		initiator.sendSelf(new TradeCompleteMessage());
	}
	
	private void handleTradeCompleteMessage(Player player) {
		TradeSession tradeSession = player.getCreatureObject().getTradeSession();
		tradeSession.sendToPartner(player.getCreatureObject(), new TradeCompleteMessage());
		tradeSession.getAccepter().setTradeSession(null);
		tradeSession.getInitiator().setTradeSession(null);
	}
	
	private void handleBeginVerificationMessage(Player player) {
		TradeSession tradeSession = player.getCreatureObject().getTradeSession();
		tradeSession.getAccepter().sendSelf(new VerifyTradeMessage());
		tradeSession.getInitiator().sendSelf(new VerifyTradeMessage());
	}
	
	private void handleTradeSessionRequest(SecureTrade packet, Player packetSender , CreatureObject initiator, CreatureObject accepter) {
		SuiMessageBox requestBox = new SuiMessageBox(SuiButtons.OK_CANCEL, "Trade Request", accepter.getOwner().getCharacterName() + " wants to trade with you.\nDo you want to accept the request?");
		requestBox.display(accepter.getOwner());
		requestBox.addOkButtonCallback("handleTradeRequest", (player, actor, event, paramenters)-> {
			if(initiator.getTradeSession() == null)
				return;
			
			accepter.setTradeSession(initiator.getTradeSession());
			accepter.sendSelf(new SecureTrade(TradeMessageType.REQUEST_TRADE, initiator.getObjectId(), accepter.getObjectId()));
			initiator.sendSelf(new SecureTrade(TradeMessageType.REQUEST_TRADE, initiator.getObjectId(), accepter.getObjectId()));
			initiator.sendSelf(new BeginTradeMessage(accepter.getObjectId()));
			accepter.sendSelf(new BeginTradeMessage(initiator.getObjectId()));
			Log.d("Trade Session Request. Type=%s  Initiator=%d  Receipient=%d PacketSenderID: %d", packet.getType(), packet.getStarterId(), packet.getAccepterId(), player.getCreatureObject().getObjectId());
		});
		requestBox.addCancelButtonCallback("handleTradeRequestDeny", (player, actor, event, paramenters)-> {
			if(packetSender.getCreatureObject().equals(initiator)){
				initiator.sendSelf(new DenyTradeMessage());
				initiator.sendSelf(new AbortTradeMessage());
			} else {
				accepter.sendSelf(new DenyTradeMessage());
				accepter.sendSelf(new AbortTradeMessage());
			}
		});
		Log.i("Player: %s sent TradeRequest to Player %s", initiator.getOwner().getCharacterName(), accepter.getOwner().getCharacterName());
	}
	
	private void sendSystemMessage(Player player, String str) {
		new ChatBroadcastIntent(player, "@ui_trade:" + str).broadcast();
	}
}