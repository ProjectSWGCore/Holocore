/***********************************************************************************
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
package network.packets.swg.zone.spatial;

import java.util.List;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;
import services.map.MapLocation;

public class GetMapLocationsResponseMessage extends SWGPacket {
	public static final int CRC = getCrc("GetMapLocationsResponseMessage");

	private String planet;
	private List<MapLocation> updatedStaticLocations;
	private List<MapLocation> updatedDynamicLocations;
	private List<MapLocation> updatedPersistLocations;
	private int staticLocVersion;
	private int dynamicLocVersion;
	private int persistentLocVersion;
	
	public GetMapLocationsResponseMessage() {
		
	}
	
	public GetMapLocationsResponseMessage(String planet,
	                                      List<MapLocation> staticLocs, List<MapLocation> dynamicLocs, List<MapLocation> persistLocs,
	                                      int staticLocVersion, int dynamicLocVersion, int persistLocVersion) {
		this.planet = planet;
		this.updatedStaticLocations = staticLocs;
		this.updatedDynamicLocations = dynamicLocs;
		this.updatedPersistLocations = persistLocs;
		this.staticLocVersion = staticLocVersion;
		this.dynamicLocVersion = dynamicLocVersion;
		this.persistentLocVersion = persistLocVersion;
	}

	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		planet					= data.getAscii();
		updatedStaticLocations = data.getList(MapLocation.class);
		updatedDynamicLocations = data.getList(MapLocation.class);
		updatedPersistLocations = data.getList(MapLocation.class);
		staticLocVersion		= data.getInt();
		dynamicLocVersion		= data.getInt();
		persistentLocVersion	= data.getInt();
	}

	@Override
	public NetBuffer encode() {
		int size = planet.length() + 32;
		
		// Get size of data
		if (updatedStaticLocations != null) {
			for (MapLocation location : updatedStaticLocations) {
				byte[] data = location.encode();
				size += data.length;
			}
		}

		if (updatedDynamicLocations != null) {
			for (MapLocation location : updatedDynamicLocations) {
				byte[] data = location.encode();
				size += data.length;
			}
		}

		if (updatedPersistLocations != null) {
			for (MapLocation location : updatedPersistLocations) {
				byte[] data = location.encode();
				size += data.length;
			}
		}

		// Create the packet
		NetBuffer data = NetBuffer.allocate(size);
		data.addShort(8);
		data.addInt(CRC);

		data.addAscii(planet);

		data.addList(updatedStaticLocations);
		data.addList(updatedDynamicLocations);
		data.addList(updatedPersistLocations);

		data.addInt(staticLocVersion);
		data.addInt(dynamicLocVersion);
		data.addInt(persistentLocVersion);
		return data;
	}

	@Override
	public String toString() {
		return "[GetMapLocationsResponseMessage] " +
				"staticVersion=" + staticLocVersion + "dynamicVersion=" + dynamicLocVersion + "persistVersion=" + persistentLocVersion;
	}
}
