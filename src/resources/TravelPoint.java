package resources;

import java.util.List;

import services.galaxy.TravelService.TravelInfo;

public final class TravelPoint {
	
	private final String name;
	private final Location location;
	private final List<TravelInfo> allowedRoutesForPoint;
	private final int additionalCost; // Additional cost. Perhaps based on distance from source to destination?
	private final boolean reachable;
	
	public TravelPoint(String name, Location location, List<TravelInfo> allowedRoutesForPoint, int additionalCost) {
		this.name = name;
		this.location = location;
		this.allowedRoutesForPoint = allowedRoutesForPoint;
		this.additionalCost = additionalCost;
		reachable = true;	// Not sure which effect this has on the client.
	}
	
	public String getName() {
		return name;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public int ticketPrice(Terrain arrivalPlanet) {
		int foundPrice = 0;
		
		for(TravelInfo travelInfo : allowedRoutesForPoint) {
			if(travelInfo.getTerrain() == arrivalPlanet) {
				foundPrice = travelInfo.getPrice();
				break;
			}
		}
		
		return foundPrice;
	}
	
	public int getAdditionalCost() {
		return additionalCost;
	}
	
	public int totalTicketPrice(Terrain arrivalPlanet) {
		return ticketPrice(arrivalPlanet) + getAdditionalCost();
	}
	
	public boolean isReachable() {
		return reachable;
	}
	
	public String suiFormat() {
		return String.format("@planet_n:%s -- %s", location.getTerrain().getName(), name);
	}
}
