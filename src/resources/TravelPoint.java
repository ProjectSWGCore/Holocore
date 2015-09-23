package resources;

public class TravelPoint {

	private String name;
	private Location location;
	private int ticketPrice;
	
	public TravelPoint(String name, Location location, int ticketPrice) {
		this.name = name;
		this.location = location;
		this.ticketPrice = ticketPrice;
	}
	
	public String getName() {
		return name;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public int getTicketPrice() {
		return ticketPrice;
	}
	
}
