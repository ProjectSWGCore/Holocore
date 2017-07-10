package services.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.projectswg.common.debug.Log;

import network.packets.swg.SWGPacket;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

public class TradeSession {

	private final List<SWGObject> initiatorTradeItems;
	private final List<SWGObject> accepterTradeItems;
	private final CreatureObject initiator;
	private final CreatureObject accepter;
	private final AtomicInteger moneyAmountInitiator;
	private final AtomicInteger moneyAmountAccepter;

	public TradeSession(CreatureObject initiator, CreatureObject accepter) {
		this.moneyAmountInitiator = new AtomicInteger();
		this.moneyAmountAccepter = new AtomicInteger();
		this.initiator = initiator;
		this.accepter = accepter;
		this.initiatorTradeItems = new ArrayList<SWGObject>();
		this.accepterTradeItems = new ArrayList<SWGObject>();
	}

	public void removeFromItemList(CreatureObject requester, long objectId) {
		if (requester.equals(this.accepter)) {
			this.initiatorTradeItems.remove(objectId);
		} else {
			this.accepterTradeItems.remove(objectId);
		}
	}

	public CreatureObject getTradePartner(CreatureObject self) {
		if (self.equals(this.accepter)) {
			return this.accepter;
		} else if(self.equals(this.initiator)) {
			return this.initiator;
		} else {
	        Log.w("Invalid trade item owner for session: %s  (initiator=%s, accepter=%s)", self, initiator, accepter);
			return self;
	    }
	}		
	
	public List<SWGObject> getFromItemList(CreatureObject creature) {
		if(creature.equals(this.accepter)){
			return Collections.unmodifiableList(this.initiatorTradeItems);
		} else {
			return Collections.unmodifiableList(this.accepterTradeItems);
		}
	}

	public CreatureObject getInitiator() {
		return this.initiator;
	}

	public CreatureObject getAccepter() {
		return this.accepter;
	}

	public void addItem(CreatureObject self, SWGObject tradeObject) {
	    if (self.equals(this.initiator)) {
	        this.initiatorTradeItems.add(tradeObject);
	        System.out.println("Item added to Initiatorlist");
	    } else if (self.equals(this.accepter)) {
	    	this.accepterTradeItems.add(tradeObject);
	    	System.out.println("Item added to Accepterlist");
	    } else {
	        Log.w("Invalid trade item owner for session: %s  (initiator=%s, accepter=%s)", self, initiator, accepter);
	    }
	}
	
	public void sendToPartner(CreatureObject creature, SWGPacket packet) {
		TradeSession tradeSession = creature.getTradeSession();
		if(creature.getObjectId() != tradeSession.getAccepter().getObjectId()){
			tradeSession.getAccepter().getOwner().sendPacket(packet);
		} else {
			tradeSession.getInitiator().getOwner().sendPacket(packet);
		}		
	}
	
	public void setMoneyAmount(CreatureObject creature, int amount){
		if(creature.equals(this.initiator)){
			this.moneyAmountInitiator.set(amount);
		} else if (creature.equals(this.initiator)){
			this.moneyAmountAccepter.set(amount);
		}
	}

	public AtomicInteger getMoneyAmount(CreatureObject creature) {
		if(creature.equals(this.initiator)){
			return this.moneyAmountInitiator;
		} else if (creature.equals(this.initiator)){
			return this.moneyAmountAccepter;
		}
		return this.moneyAmountAccepter;
	}
	
	public void moveToPartnerInventory(CreatureObject partner, List<SWGObject> fromItemList) {
		for (SWGObject tradeObject : getFromItemList(partner)) {
			tradeObject.moveToContainer(partner.getSlottedObject("inventory"));
		}			
	}
}