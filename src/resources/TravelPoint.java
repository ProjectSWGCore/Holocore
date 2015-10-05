package resources;

import java.util.List;

import resources.objects.creature.CreatureObject;
import services.galaxy.TravelService.TravelInfo;

public final class TravelPoint {
	
	private final String name;
	private final Location location;
	private final List<TravelInfo> allowedRoutesForPoint;
	private final int additionalCost; // Additional cost. Perhaps based on distance from source to destination?
	private final boolean reachable;
	private CreatureObject shuttle;
	private final boolean starport;
	
	public TravelPoint(String name, Location location, List<TravelInfo> allowedRoutesForPoint, int additionalCost, boolean starport) {
		this.name = name;
		this.location = location;
		this.allowedRoutesForPoint = allowedRoutesForPoint;
		this.additionalCost = additionalCost;
		this.starport = starport;
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
	
	public boolean isStarport() {
		return starport;
	}
	
	public boolean isReachable() {
		return reachable;
	}
	
	public CreatureObject getShuttle() {
		return shuttle;
	}

	public void setShuttle(CreatureObject shuttle) {
		this.shuttle = shuttle;
	}
	
	public String getSuiFormat() {
		return String.format("@planet_n:%s -- %s", location.getTerrain().getName(), name);
	}
}
