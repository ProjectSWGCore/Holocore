package services.trade;

import com.projectswg.common.control.Service;
import com.projectswg.common.debug.Log;

import intents.PlayerEventIntent;
import intents.network.GalacticPacketIntent;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.object_controller.SecureTrade;
import network.packets.swg.zone.trade.AbortTradeMessage;
import network.packets.swg.zone.trade.AcceptTransactionMessage;
import network.packets.swg.zone.trade.AddItemMessage;
import network.packets.swg.zone.trade.BeginTradeMessage;
import network.packets.swg.zone.trade.DenyTradeMessage;
import network.packets.swg.zone.trade.UnAcceptTransactionMessage;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.sui.SuiButtons;
import resources.sui.SuiMessageBox;
import services.objects.ObjectManager;

public class TradeService extends Service {
	
	public TradeService() {
		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
	}

	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_LOGGED_OUT:
				break;
			case PE_FIRST_ZONE:
			default:
				break;
		}
	}

	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();
		/*if (gpi.getPacket() instanceof SWGPacket)
		    Log.d("RX Packet: %s", ((SWGPacket) gpi.getPacket()).getPacketType());*/
				
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
			handleAddItemMessage((AddItemMessage) packet, gpi.getPlayer());
		}
	}

	private void handleSecureTrade(SecureTrade packet, Player player, ObjectManager objectManager) {
		CreatureObject initiator = player.getCreatureObject();
		CreatureObject accepter = (CreatureObject) objectManager.getObjectById(packet.getAccepterId());
		TradeSession tradeSession = new TradeSession(initiator, accepter);
		initiator.setTradeSession(tradeSession);
		initiator.getTradeSession().setTradePartner(accepter);
		handleTradeSessionRequest(packet, player, initiator, accepter);
		Log.d("Trade Session Request. Type=%s  Initiator=%d  Receipient=%d PacketSenderID: %d", packet.getType(), packet.getStarterId(), packet.getAccepterId(), player.getCreatureObject().getObjectId());
	}

	private void handleAbortTradeMessage(Player player) {	
		player.getCreatureObject().getTradeSession().getTradePartner().sendSelf(new AbortTradeMessage());
		player.getCreatureObject().getTradeSession().getTradePartner().sendSelf(new DenyTradeMessage());
		player.getCreatureObject().sendSelf(new AbortTradeMessage());
		player.getCreatureObject().sendSelf(new DenyTradeMessage());
	}
	
	private void handleDenyTradeMessage(Player player) {
		if(player.getCreatureObject().getObjectId() != player.getCreatureObject().getTradeSession().getTradePartner().getObjectId()){
			player.getCreatureObject().getTradeSession().getTradePartner().sendSelf(new DenyTradeMessage());
		} else{
			player.getCreatureObject().sendSelf(new DenyTradeMessage());
		}		
	}

	private void handleAcceptTransactionMessage(Player player) {
		if(player.getCreatureObject().getObjectId() != player.getCreatureObject().getTradeSession().getTradePartner().getObjectId()){
			player.getCreatureObject().getTradeSession().getTradePartner().sendSelf(new AcceptTransactionMessage());
		} else{
			player.getCreatureObject().sendSelf(new AcceptTransactionMessage());
		}			
	}
	
	private void handleUnAcceptTransactionMessage(Player player) {
		if(player.getCreatureObject().getObjectId() != player.getCreatureObject().getObjectId()){
			player.getCreatureObject().getTradeSession().getTradePartner().sendSelf(new UnAcceptTransactionMessage());
		} else{
			player.getCreatureObject().sendSelf(new UnAcceptTransactionMessage());
		}
	}
	
	private void handleAddItemMessage(AddItemMessage packet, Player player) {		
		if(player.getCreatureObject().getObjectId() != player.getCreatureObject().getTradeSession().getTradePartner().getObjectId()){
			player.getCreatureObject().getTradeSession().addToAccepterList(packet.getObjectId());
			for (long objectId : player.getCreatureObject().getTradeSession().getFromAccepterList()) {
				player.getCreatureObject().getTradeSession().getTradePartner().sendSelf(new AddItemMessage(objectId));	
			}	
		} else {
			player.getCreatureObject().getTradeSession().addToInitiatorList(packet.getObjectId());
			for (long objectId : player.getCreatureObject().getTradeSession().getFromInitiatorList()) {
				player.getCreatureObject().sendSelf(new AddItemMessage(objectId));				
			}
		}
	}
	
	private void handleTradeSessionRequest(SecureTrade packet, Player packetSender , CreatureObject initiator, CreatureObject accepter) {		
		SuiMessageBox requestBox = new SuiMessageBox(SuiButtons.OK_CANCEL, "Trade Request",	accepter.getOwner().getCharacterName() + " wants to trade with you.\nDo you want to accept the request?");
		requestBox.display(accepter.getOwner());
		requestBox.addOkButtonCallback("handleTradeRequest", (player, actor, event, paramenters)-> {
			if(initiator.getTradeSession().getTradePartner() != null && initiator.getTradeSession().getTradePartner().equals(accepter)){
				accepter.setTradeSession(initiator.getTradeSession());
				accepter.getTradeSession().setTradePartner(initiator);		
				initiator.sendSelf(new BeginTradeMessage(accepter.getObjectId()));
				accepter.sendSelf(new BeginTradeMessage(initiator.getObjectId()));
				Log.d("Trade Session Request. Type=%s  Initiator=%d  Receipient=%d PacketSenderID: %d", packet.getType(), packet.getStarterId(), packet.getAccepterId(), player.getCreatureObject().getObjectId());
			}
		});
		requestBox.addCancelButtonCallback("handleTradeRequestDeny", (player, actor, event, paramenters)-> {
			if(packetSender.getCreatureObject().getObjectId() != accepter.getObjectId()){
				initiator.sendSelf(new DenyTradeMessage());
				initiator.sendSelf(new AbortTradeMessage());
			} else {
				accepter.sendSelf(new DenyTradeMessage());
				accepter.sendSelf(new AbortTradeMessage());
			}			
		});
		Log.i("Player: %s sent TradeRequest to Player %s", initiator.getOwner().getCharacterName(), accepter.getOwner().getCharacterName());
	}	
}