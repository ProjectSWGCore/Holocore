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

package com.projectswg.holocore.services.trade;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.SecureTrade;
import com.projectswg.common.network.packets.swg.zone.trade.*;
import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.network.InboundPacketIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.sui.SuiButtons;
import com.projectswg.holocore.resources.sui.SuiMessageBox;
import com.projectswg.holocore.services.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.ArrayList;
import java.util.List;

public class TradeService extends Service {
	
	private final List<TradeSession> tradeSessions;
	
	public TradeService() {
		tradeSessions = new ArrayList<>();
	}
	
	@Override
	public boolean stop() {
		for (TradeSession tradeSession : tradeSessions) {
			tradeSession.abortTrade();
		}
		return super.stop();
	}
	
	@IntentHandler
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
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		
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
			handleVerifyTradeMessage(gpi.getPlayer());
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
		handleTradeSessionRequest(player, initiator, accepter);
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
		
		tradeSession.setMoneyAmount(creature, SWGPacket.getMoneyAmount() & 0x00000000FFFFFFFFL);
	}
	
	private void handleVerifyTradeMessage(Player player) {
		CreatureObject creature = player.getCreatureObject();
		TradeSession tradeSession = creature.getTradeSession();
		
		if (!verifyTradeSession(tradeSession, creature))
			return;
		
		tradeSession.setTradeVerified(creature);
	}
	
	private void handleTradeSessionRequest(Player SWGPacketSender, CreatureObject initiator, CreatureObject accepter) {
		SuiMessageBox requestBox = new SuiMessageBox(SuiButtons.OK_CANCEL, "Trade Request", initiator.getObjectName() + " wants to trade with you.\nDo you want to accept the request?");
		requestBox.addOkButtonCallback("handleTradeRequest", (event, paramenters) -> {
			TradeSession tradeSession = initiator.getTradeSession();
			if (tradeSession == null)
				return;
			
			accepter.setTradeSession(tradeSession);
			tradeSession.beginTrade();
		});
		requestBox.addCancelButtonCallback("handleTradeRequestDeny", (event, paramenters) -> SWGPacketSender.sendPacket(new DenyTradeMessage(), new AbortTradeMessage()));
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
