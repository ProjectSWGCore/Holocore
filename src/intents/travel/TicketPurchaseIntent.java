package intents.travel;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;

public final class TicketPurchaseIntent extends Intent {

	public static final String TYPE = "TicketPurchaseIntent";
	
	private final CreatureObject purchaser;
	private final boolean roundTrip;
	private final String destinationName;
	private final String destinationPlanet;
	
	public TicketPurchaseIntent(CreatureObject purchaser, String destinationPlanet, String destinationName, boolean roundTrip) {
		super(TYPE);
		this.purchaser = purchaser;
		this.destinationPlanet = destinationPlanet;
		this.destinationName = destinationName;
		this.roundTrip = roundTrip;
	}
	
	public CreatureObject getPurchaser() {
		return purchaser;
	}
	
	public boolean isRoundTrip() {
		return roundTrip;
	}
	
	public String getDestinationPlanet() {
		return destinationPlanet;
	}
	
	public String getDestinationName() {
		return destinationName;
	}
	
}
