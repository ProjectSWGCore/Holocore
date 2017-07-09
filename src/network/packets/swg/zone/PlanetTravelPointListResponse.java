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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;
import services.galaxy.travel.TravelPoint;

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
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(calculateSize());
		
		data.addShort(6); // Operand count
		data.addInt(CRC); // CRC
		data.addAscii(planetName);	// ASCII planet name
		
		data.addInt(travelPoints.size()); // List size
		for (TravelPoint tp : travelPoints) // Point names
			data.addAscii(tp.getName());
		
		data.addInt(travelPoints.size()); // List size
		for (TravelPoint tp : travelPoints) { // Point coordinates
			data.addFloat((float) tp.getLocation().getX());
			data.addFloat((float) tp.getLocation().getY());
			data.addFloat((float) tp.getLocation().getZ());
		}
		
		data.addInt(additionalCosts.size()); // List size
		for (int additionalCost : additionalCosts) { // additional costs
			data.addInt(additionalCost);
		}
		
		data.addInt(travelPoints.size()); // List size
		for (TravelPoint tp : travelPoints) { // reachable
			data.addBoolean(tp.isReachable());
		}
		
		return data;
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		planetName = data.getAscii();
		List<String> pointNames = data.getList(StringType.ASCII);
		List<Point3D> points = data.getList(Point3D.class);
		int[] additionalCosts = data.getIntArray();
		data.getBooleanArray(); // reachable
		
		for (int additionalCost : additionalCosts) {
			this.additionalCosts.add(additionalCost * 2);
		}
		
		for (int i = 0; i < pointNames.size(); i++) {
			String pointName = pointNames.get(i);
			Point3D point = points.get(i);
			
			travelPoints.add(new TravelPoint(pointName, new Location(point.getX(), point.getY(), point.getZ(), Terrain.getTerrainFromName(planetName)), null, isStarport(pointName)));
		}
	}
	
	private boolean isStarport(String pointName) {
		return pointName.endsWith(" Starport") || pointName.endsWith(" Spaceport") || pointName.split(" ").length == 2;
	}
	
	private int calculateSize() {
		int size = Integer.BYTES * 5 + // CRC, 4x travelpoint list size
				Short.BYTES * 2 +	// operand count + ascii string for planet name
				travelPoints.size() * (3 * Float.BYTES) + // all the floats
				travelPoints.size() * Integer.BYTES + // prices
				travelPoints.size() * Byte.BYTES; // the "reachable" booleans
		
		for (TravelPoint tp : travelPoints)
			size += tp.getName().length() + Short.BYTES; // length of each actual name + a short to indicate name length
			
		size += planetName.length();
		
		return size;
	}
	
}
