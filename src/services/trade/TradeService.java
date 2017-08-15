package services.trade;

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.SecureTrade;
import com.projectswg.common.network.packets.swg.zone.trade.AbortTradeMessage;
import com.projectswg.common.network.packets.swg.zone.trade.AcceptTransactionMessage;
import com.projectswg.common.network.packets.swg.zone.trade.AddItemMessage;
import com.projectswg.common.network.packets.swg.zone.trade.DenyTradeMessage;
import com.projectswg.common.network.packets.swg.zone.trade.GiveMoneyMessage;
import com.projectswg.common.network.packets.swg.zone.trade.UnAcceptTransactionMessage;
import com.projectswg.common.network.packets.swg.zone.trade.VerifyTradeMessage;

import intents.PlayerEventIntent;
import intents.chat.SystemMessageIntent;
import intents.network.GalacticPacketIntent;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.sui.SuiButtons;
import resources.sui.SuiMessageBox;
import services.objects.ObjectManager;
import services.objects.ObjectManager.ObjectLookup;

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
		SWGPacket SWGPacket = gpi.getPacket();
		
		if (SWGPacket instanceof SecureTrade) {
			handleSecureTrade((SecureTrade) SWGPacket, gpi.getPlayer());
		} else if (SWGPacket instanceof AbortTradeMessage) {
			handleAbortTradeMessage(gpi.getPlayer());
		} else if (SWGPacket instanceof DenyTradeMessage) {
			handleDenyTradeMessage(gpi.getPlayer());
		} else if (SWGPacket instanceof AcceptTransactionMessage) {
			handleAcceptTransactionMessage(gpi.getPlayer());
		} else if (SWGPacket instanceof UnAcceptTransactionMessage) {
			handleUnAcceptTransactionMessage(gpi.getPlayer());
		} else if (SWGPacket instanceof AddItemMessage) {
			handleAddItemMessage((AddItemMessage) SWGPacket, gpi.getPlayer());
		} else if (SWGPacket instanceof GiveMoneyMessage) {
			handleGiveMoneyMessage((GiveMoneyMessage) SWGPacket, gpi.getPlayer());
		} else if (SWGPacket instanceof VerifyTradeMessage) {
			handleVerifyTradeMessage(gpi.getPlayer(), gpi.getObjectManager());
		}
	}
	
	private void handleSecureTrade(SecureTrade SWGPacket, Player player) {
		CreatureObject initiator = player.getCreatureObject();
		SWGObject accepterObject = ObjectLookup.getObjectById(SWGPacket.getAccepterId());
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
		handleTradeSessionRequest(SWGPacket, player, initiator, accepter);
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
	
	private void handleAddItemMessage(AddItemMessage SWGPacket, Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		SWGObject tradeObject = ObjectLookup.getObjectById(SWGPacket.getObjectId());
		
		if (tradeObject == null || tradeObject.hasAttribute("no_trade") || tradeObject.getSuperParent() != creature) {
			Log.w("Invalid object to trade: %s for creature: %s", tradeObject, creature);
			tradeSession.abortTrade();
			return;
		}
		
		tradeSession.addItem(creature, tradeObject);
	}
	
	private void handleGiveMoneyMessage(GiveMoneyMessage SWGPacket, Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.setMoneyAmount(creature, SWGPacket.getMoneyAmount() & 0x00000000FFFFFFFFl);
	}
	
	private void handleVerifyTradeMessage(Player player, ObjectManager objectManager) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.setTradeVerified(creature);
	}
	
	private void handleTradeSessionRequest(SecureTrade SWGPacket, Player SWGPacketSender, CreatureObject initiator, CreatureObject accepter) {
		SuiMessageBox requestBox = new SuiMessageBox(SuiButtons.OK_CANCEL, "Trade Request", initiator.getObjectName() + " wants to trade with you.\nDo you want to accept the request?");
		requestBox.addOkButtonCallback("handleTradeRequest", (event, paramenters) -> {
			TradeSession tradeSession = initiator.getTradeSession();
			if (tradeSession == null)
				return;
			
			accepter.setTradeSession(tradeSession);
			tradeSession.beginTrade();
		});
		requestBox.addCancelButtonCallback("handleTradeRequestDeny", (event, paramenters) -> {
			SWGPacketSender.sendPacket(new DenyTradeMessage(), new AbortTradeMessage());
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
		SystemMessageIntent.broadcastPersonal(player, "@ui_trade:" + str);
	}
	
}
