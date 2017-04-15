/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package network.packets.swg.zone;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.encoding.StringType;

import network.packets.swg.SWGPacket;
import resources.TravelPoint;

public class PlanetTravelPointListResponse extends SWGPacket {

	public static final int CRC = getCrc("PlanetTravelPointListResponse");
	
	private Collection<TravelPoint> travelPoints;
	private String planetName;
	private Collection<Integer> additionalCosts;
	
	public PlanetTravelPointListResponse() {
		this("", new ArrayList<>(), new ArrayList<>());
	}
	
	public PlanetTravelPointListResponse(String planetName, Collection<TravelPoint> travelPoints, Collection<Integer> additionalCosts) {
		this.planetName = planetName;
		this.travelPoints = travelPoints;
		this.additionalCosts = additionalCosts;
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(calculateSize());
		
		addShort(data, 6); // Operand count
		addInt(data, CRC); // CRC
		addAscii(data, planetName);	// ASCII planet name
		
		addInt(data, travelPoints.size()); // List size
		for(TravelPoint tp : travelPoints) // Point names
			addAscii(data, tp.getName());
		
		addInt(data, travelPoints.size()); // List size
		for(TravelPoint tp : travelPoints) { // Point coordinates
			addFloat(data, (float) tp.getLocation().getX());
			addFloat(data, (float) tp.getLocation().getY());
			addFloat(data, (float) tp.getLocation().getZ());
		}
		
		addInt(data, additionalCosts.size()); // List size
		for(int additionalCost : additionalCosts) { // additional costs
			addInt(data, additionalCost <= 0 ? additionalCost + 50 : additionalCost / 2);
		}
		
		addInt(data, travelPoints.size()); // List size
		for(TravelPoint tp : travelPoints) { // reachable
			addBoolean(data, tp.isReachable());
		}
		
		return data;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		planetName = getAscii(data);
		List<String> pointNames = getList(data, StringType.ASCII);
		List<Point3D> points = getList(data, Point3D.class);
		int[] additionalCosts = getIntArray(data);
		boolean[] pointsReachable = getBooleanArray(data);
		
		for(int additionalCost : additionalCosts)
			this.additionalCosts.add(additionalCost * 2);
		
		for(int i = 0; i < pointNames.size(); i++) {
			String pointName = pointNames.get(i);
			Point3D point = points.get(i);
			boolean reachable = pointsReachable[i];
			
			travelPoints.add(new TravelPoint(pointName, new Location(point.getX(), point.getY(), point.getZ(), Terrain.getTerrainFromName(planetName)), isStarport(pointName), reachable));
		}
	}
	
	private boolean isStarport(String pointName) {
		return pointName.endsWith(" Starport") || pointName.endsWith(" Spaceport") || pointName.split(" ").length == 2;
	}
	
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
