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
import network.packets.swg.zone.trade.AbortTradeMessage;
import network.packets.swg.zone.trade.AcceptTransactionMessage;
import network.packets.swg.zone.trade.AddItemMessage;
import network.packets.swg.zone.trade.DenyTradeMessage;
import network.packets.swg.zone.trade.GiveMoneyMessage;
import network.packets.swg.zone.trade.UnAcceptTransactionMessage;
import network.packets.swg.zone.trade.VerifyTradeMessage;
import resources.Posture;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.sui.SuiButtons;
import resources.sui.SuiMessageBox;
import services.objects.ObjectManager;
import services.objects.ObjectManager.ObjectLookup;

/*
 * Server Packets: 
 *   class BeginTradeMessage	: ServerSecureTrade::beginTrading()
 *   class AbortTradeMessage	: ServerSecureTrade::cancelTrade()
 *   
 *   class AddItemMessage		: ServerSecureTrade::addItem() [forwarded]
 *   class AddItemFailedMessage	: ServerSecureTrade::addItem()
 *   class RemoveItemMessage	: --commented out--
 *   class GiveMoneyMessage		: ServerSecureTrade::giveMoney() [forwarded]
 *   class TradeCompleteMessage	: ServerSecureTrade::completeTrade()
 *   
 *   class AcceptTransactionMessage		: ServerSecureTrade::acceptOffer() [forwarded]
 *   class UnAcceptTransactionMessage	: ServerSecureTrade::unacceptOffer() [forwarded]
 *   
 *   class VerifyTradeMessage			: ServerSecureTrade::verifyTrade() [forwarded]
 *   class DenyTradeMessage				: ServerSecureTrade::rejectOffer() [forwarded]
 *   class BeginVerificationMessage		: ServerSecureTrade::beginVerification()
 * Notes:
 *   + A sends TMI_RequestTrade
 *     + S sends TMI_TradeRequested to B
 *   + B sends AcceptTradeMessage
 *     + S sends BeginTradeMessage to Act
 *     
 *     + S sends BeginTradeMessage to B
 *   ----- In Trade -----
 *       + A/B sends AddItemMessage
 *         + S either sends AddItemFailedMessage or forwards packet
 *       + A/B sends GiveMoneyMessage
 *         + S forwards packet
 *       + A/B sends AcceptTransactionMessage
 *         + S forwards packet
 *         + if both accept, S sends BeginVerifyMessage
 *       + A/B sends VerifyTradeMessage
 *         + S forwards packet
 *       + S sends TradeCompleteMessage when done
 *       + 
 */
public class TradeService extends Service {
	
	private final List<TradeSession> tradeSessions;
	
	public TradeService() {
		tradeSessions = new ArrayList<TradeSession>();
		
		registerForIntent(GalacticPacketIntent.class, this::handleGalacticPacketIntent);
		registerForIntent(PlayerEventIntent.class, this::handlePlayerEventIntent);
	}
	
	@Override
	public boolean stop() {
		for (TradeSession tradeSession : tradeSessions) {
			tradeSession.abortTrade();
		}
		return super.stop();
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		if (pei.getPlayer().getCreatureObject() == null)
			return;
		TradeSession tradeSession = pei.getPlayer().getCreatureObject().getTradeSession();
		if (tradeSession == null)
			return;
		
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
			case PE_LOGGED_OUT:
				tradeSession.abortTrade();
				break;
			default:
				break;
		}
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();
		
		if (packet instanceof SecureTrade) {
			handleSecureTrade((SecureTrade) packet, gpi.getPlayer());
		} else if (packet instanceof AbortTradeMessage) {
			handleAbortTradeMessage(gpi.getPlayer());
		} else if (packet instanceof DenyTradeMessage) {
			handleDenyTradeMessage(gpi.getPlayer());
		} else if (packet instanceof AcceptTransactionMessage) {
			handleAcceptTransactionMessage(gpi.getPlayer());
		} else if (packet instanceof UnAcceptTransactionMessage) {
			handleUnAcceptTransactionMessage(gpi.getPlayer());
		} else if (packet instanceof AddItemMessage) {
			handleAddItemMessage((AddItemMessage) packet, gpi.getPlayer());
		} else if (packet instanceof GiveMoneyMessage) {
			handleGiveMoneyMessage((GiveMoneyMessage) packet, gpi.getPlayer());
		} else if (packet instanceof VerifyTradeMessage) {
			handleVerifyTradeMessage(gpi.getPlayer(), gpi.getObjectManager());
		}
	}
	
	private void handleSecureTrade(SecureTrade packet, Player player) {
		CreatureObject initiator = player.getCreatureObject();
		SWGObject accepterObject = ObjectLookup.getObjectById(packet.getAccepterId());
		if (!(accepterObject instanceof CreatureObject) || !((CreatureObject) accepterObject).isPlayer()) {
			sendSystemMessage(initiator.getOwner(), "start_fail_target_not_player");
			return;
		}
		CreatureObject accepter = (CreatureObject) accepterObject;
		
		if (initiator.isInCombat() || accepter.isInCombat()) {
			sendSystemMessage(initiator.getOwner(), "request_player_unreachable_no_obj");
			return;
		}
		
		if (initiator.getPosture() == Posture.INCAPACITATED || accepter.getPosture() == Posture.INCAPACITATED) {
			sendSystemMessage(initiator.getOwner(), "player_incapacitated");
			return;
		}
		
		if (initiator.getPosture() == Posture.DEAD || accepter.getPosture() == Posture.DEAD) {
			sendSystemMessage(initiator.getOwner(), "player_dead");
			return;
		}
		
		TradeSession tradeSession = new TradeSession(initiator, accepter);
		tradeSessions.add(tradeSession);
		TradeSession oldSession = initiator.getTradeSession();
		initiator.setTradeSession(tradeSession);
		if (oldSession != null)
			oldSession.abortTrade();
		handleTradeSessionRequest(packet, player, initiator, accepter);
	}
	
	private void handleAbortTradeMessage(Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.abortTrade();
	}
	
	private void handleDenyTradeMessage(Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.denyTrade();
	}
	
	private void handleAcceptTransactionMessage(Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.setTradeAccepted(creature, true);
	}
	
	private void handleUnAcceptTransactionMessage(Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.setTradeAccepted(creature, false);
	}
	
	private void handleAddItemMessage(AddItemMessage packet, Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		SWGObject tradeObject = ObjectLookup.getObjectById(packet.getObjectId());
		
		if (tradeObject == null || tradeObject.hasAttribute("no_trade") || tradeObject.getSuperParent() != creature) {
			Log.w("Invalid object to trade: %s for creature: %s", tradeObject, creature);
			tradeSession.abortTrade();
			return;
		}
		
		tradeSession.addItem(creature, tradeObject);
	}
	
	private void handleGiveMoneyMessage(GiveMoneyMessage packet, Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.setMoneyAmount(creature, packet.getMoneyAmount() & 0x00000000FFFFFFFFl);
	}
	
	private void handleVerifyTradeMessage(Player player, ObjectManager objectManager) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.setTradeVerified(creature);
	}
	
	private void handleTradeSessionRequest(SecureTrade packet, Player packetSender, CreatureObject initiator, CreatureObject accepter) {
		SuiMessageBox requestBox = new SuiMessageBox(SuiButtons.OK_CANCEL, "Trade Request", initiator.getObjectName() + " wants to trade with you.\nDo you want to accept the request?");
		requestBox.addOkButtonCallback("handleTradeRequest", (player, actor, event, paramenters) -> {
			TradeSession tradeSession = initiator.getTradeSession();
			if (tradeSession == null)
				return;
			
			accepter.setTradeSession(tradeSession);
			tradeSession.beginTrade();
		});
		requestBox.addCancelButtonCallback("handleTradeRequestDeny", (player, actor, event, paramenters) -> {
			packetSender.sendPacket(new DenyTradeMessage(), new AbortTradeMessage());
		});
		requestBox.display(accepter.getOwner());
	}
	
	private boolean verifyTradeSession(TradeSession session, CreatureObject creature) {
		if (session == null) {
			Log.w("Invalid TradeSession. Creature %s: ", creature);
			Player owner = creature.getOwner();
			if (owner != null)
				owner.sendPacket(new DenyTradeMessage(), new AbortTradeMessage());
			return false;
		}
		return true;
	}
	
	private void sendSystemMessage(Player player, String str) {
		ChatBroadcastIntent.broadcastPersonal(player, "@ui_trade:" + str);
	}
	
}
