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
	private final AtomicInteger initiatorMoneyAmount;
	private final AtomicInteger accepterMoneyAmount;

	public TradeSession(CreatureObject initiator, CreatureObject accepter) {
		this.initiatorTradeItems = new ArrayList<>();
		this.accepterTradeItems = new ArrayList<>();
		this.initiator = initiator;
		this.accepter = accepter;
		this.initiatorMoneyAmount = new AtomicInteger();
		this.accepterMoneyAmount = new AtomicInteger();
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
		if(creature.getObjectId() != this.getAccepter().getObjectId()){
			this.getAccepter().getOwner().sendPacket(packet);
		} else {
			this.getInitiator().getOwner().sendPacket(packet);
		}		
	}
	
	public void setMoneyAmount(CreatureObject creature, int amount){
		if(creature.equals(this.initiator)){
			this.initiatorMoneyAmount.set(amount);
		} else if (creature.equals(this.initiator)){
			this.accepterMoneyAmount.set(amount);
		}
	}

	public int getMoneyAmount(CreatureObject creature) {
		if(creature.equals(this.initiator)){
			return this.initiatorMoneyAmount.get();
		} else if (creature.equals(this.initiator)){
			return this.accepterMoneyAmount.get();
		}
		return this.accepterMoneyAmount.get();
	}
	
	public void moveToPartnerInventory(CreatureObject partner, List<SWGObject> fromItemList) {
		for (SWGObject tradeObject : fromItemList) {
			tradeObject.moveToContainer(getTradePartner(partner).getSlottedObject("inventory"));
		}			
	}
}