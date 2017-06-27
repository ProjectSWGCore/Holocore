package services.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

public class TradeSession {

	private final List<Long> initiatorTradeItems;
	private final List<Long> accepterTradeItems;
	private final CreatureObject initiator;
	private final CreatureObject accepter;
	
	public TradeSession(CreatureObject initiator, CreatureObject accepter) {		
		this.initiator = initiator;
		this.accepter = accepter;
		this.initiatorTradeItems = new ArrayList<Long>();
		this.accepterTradeItems = new ArrayList<Long>();
	}

	public void addToItemList(CreatureObject requester, long objectId){
		if(requester.equals(this.initiator)){
			this.initiatorTradeItems.add(objectId);
		} else {
			this.accepterTradeItems.add(objectId);
		}
	}
	
	public void removeFromItemList(CreatureObject requester, long objectId){
		if(requester.equals(this.initiator)){
			this.initiatorTradeItems.remove(objectId);
		} else {
			this.accepterTradeItems.remove(objectId);
		}
	}
	
	public List<Long> getFromInitiatorList(){
		return Collections.unmodifiableList(initiatorTradeItems);
	}
	
	public List<Long> getFromAccepterList(){
		return Collections.unmodifiableList(accepterTradeItems);
	}

	public CreatureObject getInitiator() {
		return initiator;
	}

	public CreatureObject getAccepter() {
		return accepter;
	}	
	
	public CreatureObject getTradePartner(CreatureObject self) {
		if (self.equals(initiator)) {
			return accepter;
		} else {
			return initiator;
		}
	}
}