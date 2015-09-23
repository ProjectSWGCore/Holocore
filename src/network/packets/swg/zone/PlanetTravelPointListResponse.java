package network.packets.swg.zone;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import resources.TravelPoint;
import network.packets.swg.SWGPacket;

public class PlanetTravelPointListResponse extends SWGPacket {

	public static final int CRC = getCrc("PlanetTravelPointListResponse");
	
	private Collection<TravelPoint> travelPoints;
	private String planetName;
	
	public PlanetTravelPointListResponse(String planetName, Collection<TravelPoint> travelPoints) {
		this.planetName = planetName;
		this.travelPoints = travelPoints;
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(calculateSize());
		
		addShort(data, 6);
		addInt(data, CRC);
		addAscii(data, planetName);
		
		addInt(data, travelPoints.size()); // List size
		for(TravelPoint tp : travelPoints) // Point names
			addAscii(data, tp.getName());
		
		addInt(data, travelPoints.size()); // List size
		for(TravelPoint tp : travelPoints) { // Point coordinates
			addFloat(data, (float) tp.getLocation().getX());
			addFloat(data, (float) tp.getLocation().getY());
			addFloat(data, (float) tp.getLocation().getZ());
		}
		
		addInt(data, travelPoints.size()); // List size
		for(TravelPoint tp : travelPoints) { // Prices
			addInt(data, tp.getTicketPrice());
		}
		
		
		addInt(data, travelPoints.size()); // List size
		for(TravelPoint tp : travelPoints) { // ? TODO boolean reachable?
			addBoolean(data, true);
		}
		
		return data;
	}
	
	// TODO implement decode()
	
	private int calculateSize() {
		int size =
				Integer.BYTES * 5 + // CRC, 4x travelpoint list size
				Short.BYTES * 2 +	// operand count + ascii string for planet name
				travelPoints.size() * (3 * Float.BYTES) + // all the floats
				travelPoints.size() * Integer.BYTES + // prices
				travelPoints.size() * Byte.BYTES; // the "reachable" booleans
		
		for(TravelPoint tp : travelPoints)
			size += tp.getName().length() + Short.BYTES; // length of each actual name + a short to indicate name length
		
		size += planetName.length();
		
		return size;
	}
	
}
