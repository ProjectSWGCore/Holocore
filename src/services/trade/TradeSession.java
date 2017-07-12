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
		if (requester.equals(accepter)) {
			synchronized (initiatorTradeItems) {
				initiatorTradeItems.remove(objectId);
			}
		} else {
			synchronized (accepterTradeItems) {
				accepterTradeItems.remove(objectId);
			}
		}
	}

	public CreatureObject getTradePartner(CreatureObject self) {
		if (self.equals(accepter)) {
			return this.accepter;
		} else if(self.equals(initiator)) {
			return initiator;
		} else {
	        Log.w("Invalid trade item owner for session: %s  (initiator=%s, accepter=%s)", self, initiator, accepter);
			return self;
	    }
	}		
	
	public List<SWGObject> getFromItemList(CreatureObject creature) {
		if(creature.equals(this.accepter)){
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

	public void addItem(CreatureObject self, SWGObject tradeObject) {		
	    if (self.equals(initiator)) {
	    	synchronized (initiatorTradeItems) {
	    		initiatorTradeItems.add(tradeObject);
	    	}
	    } else if (self.equals(accepter)) {
	    	synchronized (accepterTradeItems) {
	    		accepterTradeItems.add(tradeObject);
	    	}
	    } else {
	        Log.w("Invalid trade item owner for session: %s  (initiator=%s, accepter=%s)", self, initiator, accepter);
	    }
	}
	
	public void sendToPartner(CreatureObject creature, SWGPacket packet) {
		if(creature.getObjectId() != getAccepter().getObjectId()){
			getAccepter().getOwner().sendPacket(packet);
		} else {
			getInitiator().getOwner().sendPacket(packet);
		}		
	}
	
	public void setMoneyAmount(CreatureObject creature, int amount){
		if(creature.equals(initiator)){
			initiatorMoneyAmount.set(amount);
		} else if (creature.equals(initiator)){
			accepterMoneyAmount.set(amount);
		}
	}

	public int getMoneyAmount(CreatureObject creature) {
		if(creature.equals(initiator)){
			return initiatorMoneyAmount.get();
		} else if (creature.equals(initiator)){
			return accepterMoneyAmount.get();
		}
		return accepterMoneyAmount.get();
	}
	
	public void moveToPartnerInventory(CreatureObject partner, List<SWGObject> fromItemList) {
		for (SWGObject tradeObject : fromItemList) {
			tradeObject.moveToContainer(getTradePartner(partner).getSlottedObject("inventory"));
		}			
	}
}