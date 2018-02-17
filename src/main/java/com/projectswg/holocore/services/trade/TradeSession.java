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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.trade.AbortTradeMessage;
import com.projectswg.common.network.packets.swg.zone.trade.AcceptTransactionMessage;
import com.projectswg.common.network.packets.swg.zone.trade.AddItemMessage;
import com.projectswg.common.network.packets.swg.zone.trade.BeginTradeMessage;
import com.projectswg.common.network.packets.swg.zone.trade.BeginVerificationMessage;
import com.projectswg.common.network.packets.swg.zone.trade.DenyTradeMessage;
import com.projectswg.common.network.packets.swg.zone.trade.GiveMoneyMessage;
import com.projectswg.common.network.packets.swg.zone.trade.TradeCompleteMessage;
import com.projectswg.common.network.packets.swg.zone.trade.UnAcceptTransactionMessage;
import com.projectswg.common.network.packets.swg.zone.trade.VerifyTradeMessage;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;

public class TradeSession {
	
	private final TradeMember initiator;
	private final TradeMember receiver;
	private final AtomicReference<TradeStatus> status;
	
	public TradeSession(CreatureObject initiator, CreatureObject receiver) {
		Objects.requireNonNull(initiator, "Initiator cannot be null!");
		Objects.requireNonNull(receiver, "Receiver cannot be null!");
		this.initiator = new TradeMember(initiator, receiver);
		this.receiver = new TradeMember(receiver, initiator);
		this.status = new AtomicReference<>(TradeStatus.STARTED);
	}
	
	public void addItem(CreatureObject creature, SWGObject tradeObject) {
		verifyInternalState();
		verifyCreature(creature);
		getTradeMember(creature).offerItem(tradeObject);
	}
	
	public void setMoneyAmount(CreatureObject creature, long amount) {
		verifyInternalState();
		verifyCreature(creature);
		status.set(TradeStatus.TRADING);
		getTradeMember(creature).offerMoney(amount);
	}
	
	public void setTradeAccepted(CreatureObject creature, boolean accepted) {
		verifyInternalState();
		verifyCreature(creature);
		if (status.get() != TradeStatus.TRADING) {
			Log.w("Trade %s in wrong state (%s) with creature: %s", accepted ? "accepted" : "unaccepted", status.get(), creature);
			return;
		}
		getTradeMember(creature).setAccepted(accepted);
		if (accepted) {
			if (isTradeAccepted()) {
				initiator.sendPacket(new BeginVerificationMessage());
				receiver.sendPacket(new BeginVerificationMessage());
			}
		} else if (initiator.isVerified() || receiver.isVerified()) {
			abortTrade();
		}
	}
	
	public void setTradeVerified(CreatureObject creature) {
		verifyInternalState();
		verifyCreature(creature);
		if (!isTradeAccepted()) {
			abortTrade();
			return;
		}
		status.set(TradeStatus.VERIFYING);
		getTradeMember(creature).verify();
		if (isTradeVerified()) {
			status.set(TradeStatus.VERIFIED);
			completeTrade();
		}
	}
	
	public boolean isItemTraded(CreatureObject creature, SWGObject tradeObject) {
		verifyInternalState();
		verifyCreature(creature);
		return getTradeMember(creature).isItemTraded(tradeObject);
	}
	
	public boolean isTradeAccepted() {
		verifyInternalState();
		return initiator.isAccepted() && receiver.isAccepted();
	}
	
	public boolean isTradeVerified() {
		verifyInternalState();
		return initiator.isVerified() && receiver.isVerified();
	}
	
	public void beginTrade() {
		if (!setStatus(TradeStatus.STARTED, TradeStatus.TRADING)) {
			Log.w("Trade begun in wrong state (%s) with %s and %s", status.get(), initiator.getCreature().getObjectName(), receiver.getCreature().getObjectName());
			return;
		}
		initiator.sendPacket(new BeginTradeMessage(receiver.getCreature().getObjectId()));
		receiver.sendPacket(new BeginTradeMessage(initiator.getCreature().getObjectId()));
		Log.i("TradeSession started between '%s' and '%s'", initiator.getCreature().getObjectName(), receiver.getCreature().getObjectName());
	}
	
	public void denyTrade() {
		verifyInternalState();
		status.set(TradeStatus.ENDED);
		Log.i("TradeSession denied between '%s' and '%s'", initiator.getCreature().getObjectName(), receiver.getCreature().getObjectName());
		initiator.denyTrade();
		receiver.denyTrade();
		closeSession();
	}
	
	public void abortTrade() {
		verifyInternalState();
		status.set(TradeStatus.ENDED);
		Log.i("TradeSession aborted between '%s' and '%s'", initiator.getCreature().getObjectName(), receiver.getCreature().getObjectName());
		initiator.abortTrade();
		receiver.abortTrade();
		closeSession();
	}
	
	private void completeTrade() {
		verifyInternalState();
		if (!isTradeVerified() || !setStatus(TradeStatus.VERIFIED, TradeStatus.ENDED)) {
			Log.w("Invalid trade session status '%s' for completing the trade!", status.get());
			return;
		}
		
		initiator.completeTrade();
		receiver.completeTrade();
		closeSession();
	}
	
	public boolean isValidSession() {
		boolean valid = status.get() != TradeStatus.ENDED && initiator.getCreature().isLoggedInPlayer() && receiver.getCreature().isLoggedInPlayer();
		if (!valid) {
			Log.w("Invalid TradeSession. TradeStatus: %s  initiatorLoggedIn=%b  receiverLoggedIn=%b", status.get(), initiator.getCreature().isLoggedInPlayer(), receiver.getCreature().isLoggedInPlayer());
			closeSession();
		}
		return valid;
	}
	
	private void closeSession() {
		status.set(TradeStatus.ENDED);
		initiator.getCreature().setTradeSession(null);
		receiver.getCreature().setTradeSession(null);
	}
	
	private void verifyInternalState() {
		if (!isValidSession())
			throw new IllegalStateException("TradeSession is already ended!");
	}
	
	private void verifyCreature(CreatureObject creature) {
		if (!initiator.isCreature(creature) && !receiver.isCreature(creature))
			throw new IllegalArgumentException("Creature is not a part of this trade session!");
	}
	
	private TradeMember getTradeMember(CreatureObject creature) {
		if (initiator.isCreature(creature))
			return initiator;
		if (receiver.isCreature(creature))
			return receiver;
		return null;
	}
	
	
	private boolean setStatus(TradeStatus expectedStatus, TradeStatus newStatus) {
		return status.compareAndSet(expectedStatus, newStatus);
	}
	
	private enum TradeStatus {
		STARTED,
		TRADING,
		VERIFYING,
		VERIFIED,
		ENDED
	}
	
	private static class TradeMember {
		
		private final CreatureObject creature;
		private final CreatureObject partner;
		private final List<SWGObject> itemsOffered;
		private final AtomicLong moneyOffered;
		private final AtomicBoolean accepted;
		private final AtomicBoolean verified;
		
		public TradeMember(CreatureObject creature, CreatureObject partner) {
			this.creature = creature;
			this.partner = partner;
			this.itemsOffered = new CopyOnWriteArrayList<>();
			this.moneyOffered = new AtomicLong(0);
			this.accepted = new AtomicBoolean(false);
			this.verified = new AtomicBoolean(false);
		}
		
		public CreatureObject getCreature() {
			return creature;
		}
		
		public boolean isCreature(CreatureObject creature) {
			return creature != null && this.creature.equals(creature);
		}
		
		public boolean offerItem(SWGObject item) {
			if (item.getSuperParent() != creature) { // gotta have the item on ya
				return false;
			}
			invalidateAcceptance();
			itemsOffered.add(item);
			sendToPartner(new AddItemMessage(item.getObjectId()));
			return true;
		}
		
		public boolean offerMoney(long money) {
			if (money > creature.getCashBalance()) // can only trade in cash
				return false;
			invalidateAcceptance();
			moneyOffered.set(money);
			sendToPartner(new GiveMoneyMessage((int) money));
			return true;
		}
		
		public void sendPacket(SWGPacket SWGPacket) {
			creature.getOwner().sendPacket(SWGPacket);
		}
		
		public void sendToPartner(SWGPacket SWGPacket) {
			partner.getOwner().sendPacket(SWGPacket);
		}
		
		public void setAccepted(boolean accepted) {
			this.accepted.getAndSet(accepted);
			if (accepted)
				sendToPartner(new AcceptTransactionMessage());
			else
				sendToPartner(new UnAcceptTransactionMessage());
		}
		
		public void verify() {
			verified.set(true);
			sendToPartner(new VerifyTradeMessage());
		}
		
		public boolean isItemTraded(SWGObject tradeObject) {
			return itemsOffered.contains(tradeObject);
		}
		
		public boolean isAccepted() {
			return accepted.get();
		}
		
		public boolean isVerified() {
			return verified.get();
		}
		
		public void abortTrade() {
			sendPacket(new AbortTradeMessage());
		}
		
		public void denyTrade() {
			sendPacket(new DenyTradeMessage());
			sendPacket(new AbortTradeMessage());
		}
		
		public void completeTrade() {
			SWGObject inventory = partner.getSlottedObject("inventory");
			
			for (SWGObject tradeObject : itemsOffered) {
				partner.removeCustomAware(tradeObject);
				tradeObject.moveToContainer(inventory);
			}
			
			long cash = moneyOffered.get();
			creature.removeFromCashAndBank(cash);
			partner.removeFromCashAndBank(-cash);
			
			Log.d("Completed trade. [%s -> %s] Cash: %d  Items: %s", creature, partner, cash, itemsOffered);
			sendPacket(new TradeCompleteMessage());
		}
		
		public void invalidateAcceptance() {
			this.accepted.set(false);
			this.verified.set(false);
		}
		
	}
}
