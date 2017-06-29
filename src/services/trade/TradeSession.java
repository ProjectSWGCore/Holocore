package services.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.projectswg.common.debug.Log;

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

	public void removeFromItemList(CreatureObject requester, long objectId) {
		if (requester.equals(this.initiator)) {
			this.initiatorTradeItems.remove(objectId);
		} else {
			this.accepterTradeItems.remove(objectId);
		}
	}

	public CreatureObject getTradePartner(CreatureObject self) {
		if (self.equals(initiator)) {
			return accepter;
		} else {
			return initiator;
		}
	}
		
	public List<Long> getFromItemList(CreatureObject creature) {
		if(this.initiator == creature){
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

	public void addItem(CreatureObject self, long itemId) {
	    if (this.initiator == self) {
	        initiatorTradeItems.add(itemId);
	    } else if (this.accepter == self) {
	    	accepterTradeItems.add(itemId);
	    } else {
	        Log.w("Invalid trade item owner for session: %s  (initiator=%s, accepter=%s)", self, initiator, accepter);
	    }
	}
}