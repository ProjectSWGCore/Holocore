package resources;

import resources.objects.creature.CreatureObject;

public final class TravelPoint {
	
	private final String name;
	private final Location location;
	private final int additionalCost; // Additional cost. Perhaps based on distance from source to destination?
	private final boolean reachable;
	private CreatureObject shuttle;
	private final boolean starport;
	
	public TravelPoint(String name, Location location, int additionalCost, boolean starport, boolean reachable) {
		this.name = name;
		this.location = location;
		this.additionalCost = additionalCost;
		this.starport = starport;
		this.reachable = reachable;	// Not sure which effect this has on the client.
	}
	
	public String getName() {
		return name;
	}
	
	public Location getLocation() {
		return location;
	}

	public int getAdditionalCost() {
		return additionalCost;
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
