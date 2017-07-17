package services.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.projectswg.common.debug.Assert;

import network.packets.swg.SWGPacket;
import network.packets.swg.zone.trade.AbortTradeMessage;
import network.packets.swg.zone.trade.TradeCompleteMessage;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;

public class TradeSession {

	private final List<SWGObject> initiatorTradeItems;
	private final List<SWGObject> accepterTradeItems;
	private final CreatureObject initiator;
	private final CreatureObject accepter;
	private final AtomicInteger initiatorMoneyAmount;
	private final AtomicInteger accepterMoneyAmount;
	private final AtomicBoolean initiatorVerified;
	private final AtomicBoolean accepterVerified;
	private final AtomicBoolean initiatorBeginSend;
	private final AtomicBoolean accepterBeginSend;

	public TradeSession(CreatureObject initiator, CreatureObject accepter) {
		this.initiatorTradeItems = new ArrayList<>();
		this.accepterTradeItems = new ArrayList<>();
		this.initiator = Objects.requireNonNull(initiator, "Initiator cannot be null!");
		this.accepter = Objects.requireNonNull(accepter, "Accepter cannot be null!");
		this.initiatorMoneyAmount = new AtomicInteger();
		this.accepterMoneyAmount = new AtomicInteger();
		this.initiatorVerified = new AtomicBoolean();
		this.accepterVerified = new AtomicBoolean();
		this.initiatorBeginSend = new AtomicBoolean();
		this.accepterBeginSend = new AtomicBoolean();
	}

	public void removeFromItemList(CreatureObject requester, long objectId) {
		Assert.test(isInTradeSession(requester), "Creature is not a part of this trade session!");
		if(isInitiator(requester)){
			synchronized (initiatorTradeItems) {
				initiatorTradeItems.remove(objectId);
			}
		
		} else {
			synchronized (accepterTradeItems) {
				accepterTradeItems.remove(objectId);
			}
		}
	}
	
	public CreatureObject getTradePartner(CreatureObject creature) {
		Assert.test(isInTradeSession(creature), "Creature is not a part of this trade session!");
		if(isInitiator(creature)){
			return accepter;
		} else {
			return initiator;
		}
	}		
	
	public List<SWGObject> getFromItemList(CreatureObject creature) {
		Assert.test(isInTradeSession(creature), "Creature is not a part of this trade session!");
		if(isInitiator(creature)){
			return Collections.unmodifiableList(initiatorTradeItems);
		} else {
			return Collections.unmodifiableList(accepterTradeItems);
		}
	}
	
	public CreatureObject getInitiator() {
		return initiator;
	}
	
	public CreatureObject getAccepter() {
		return accepter;
	}
	
	public void addItem(CreatureObject creature, SWGObject tradeObject) {
		Assert.test(isInTradeSession(creature), "Creature is not a part of this trade session!");
		if(isInitiator(creature)){
			synchronized (initiatorTradeItems) {
				initiatorTradeItems.add(tradeObject);
			}
		} else {
			synchronized (accepterTradeItems) {
				accepterTradeItems.add(tradeObject);
			}
		}
	}
	
	public void sendToPartner(CreatureObject creature, SWGPacket packet) {
		Assert.test(isInTradeSession(creature), "Creature is not a part of this trade session!");
		if(isInitiator(creature)){
			getAccepter().getOwner().sendPacket(packet);
		} else {
			getInitiator().getOwner().sendPacket(packet);
		}		
	}
	
	public void setMoneyAmount(CreatureObject creature, int amount){
		Assert.test(isInTradeSession(creature), "Creature is not a part of this trade session!");
		if(isInitiator(creature)){
			initiatorMoneyAmount.set(amount);
		} else {
			accepterMoneyAmount.set(amount);
		}
	}

	public int getMoneyAmount(CreatureObject creature) {
		Assert.test(isInTradeSession(creature), "Creature is not a part of this trade session!");
		if(isInitiator(creature)){
			return initiatorMoneyAmount.get();
		} else {
			return accepterMoneyAmount.get();
		}
	}
	
	public void moveToPartnerInventory(CreatureObject partner, List<SWGObject> fromItemList) {
		for (SWGObject tradeObject : fromItemList) {
			tradeObject.moveToContainer(getTradePartner(partner).getSlottedObject("inventory"));
		}			
	}

	public boolean isInitiatorVerified() { 
		return initiatorVerified.get();
	}

	public boolean isAccepterVerified() { 
		return accepterVerified.get();
	}
	
	public void setInititatorVerified(boolean initiatorVerfified){
		this.initiatorVerified.set(initiatorVerfified);
	}
	
	public void setAccepterVerified(boolean accepterVerified){
		this.accepterVerified.set(accepterVerified);
	}
	
	public boolean isInitiatorBeginSend() {
		return initiatorBeginSend.get();
	}

	public boolean isAccepterBeginSend() {
		return accepterBeginSend.get();
	}
	
	public void setInititatorBeginSend(boolean inititatorBeginSend){
		this.initiatorBeginSend.set(inititatorBeginSend);
	}
	
	public void setAccepterBeginSend(boolean accepterBeginSend){
		this.accepterBeginSend.set(accepterBeginSend);
	}

	public void sendAbortTrade() { 
		Player accepter = this.accepter.getOwner();
		Player initiator = this.initiator.getOwner();
		if (accepter != null)
			accepter.sendPacket(new AbortTradeMessage(), new TradeCompleteMessage());
		if (initiator != null)
			initiator.sendPacket(new AbortTradeMessage(), new TradeCompleteMessage());
		
		if (initiator.getCreatureObject() != null)
			initiator.getCreatureObject().setTradeSession(null);
		if (accepter.getCreatureObject() != null)
			accepter.getCreatureObject().setTradeSession(null);
	}
	
	private boolean isInitiator(CreatureObject creature) {
		return creature.equals(initiator); 
	}
	
	private boolean isInTradeSession(CreatureObject creature) {
		return isInitiator(creature) || (accepter != null && creature.equals(accepter));
	}
}