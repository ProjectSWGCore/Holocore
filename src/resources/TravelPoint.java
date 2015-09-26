package resources;

import java.util.Map;

public final class TravelPoint {
	
	private String name;
	private Location location;
	private final Map<String, Integer> planetFees;	// The price of travelling is mapped to each terrain.
	private final int additionalCost; // Additional cost is calculated based on distance from source to destination.
	private final boolean reachable;
	
	public TravelPoint(String name, Location location, Map<String, Integer> planetFees, int additionalCost) {
		this.name = name;
		this.location = location;
		this.planetFees = planetFees;
		this.additionalCost = additionalCost;
		reachable = true;	// Not sure if this is ever false or which effect that has.
	}
	
	public String getName() {
		return name;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public int ticketPrice(String planetName) {
		return planetFees.get(planetName);
	}
	
	public int getAdditionalCost() {
		return additionalCost;
	}
	
	public int totalTicketPrice(String planetName) {
		return ticketPrice(planetName) + getAdditionalCost();
	}
	
	public boolean isReachable() {
		return reachable;
	}
	
	public String suiFormat() {
		return String.format("@planet_n:%s -- %s", location.getTerrain().getName(), name);
	}
}
