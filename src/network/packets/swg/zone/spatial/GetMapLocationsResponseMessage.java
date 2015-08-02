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

import network.packets.swg.SWGPacket;
import services.map.MapLocation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class GetMapLocationsResponseMessage extends SWGPacket {
	public static final int CRC = getCrc("GetMapLocationsResponseMessage");

	private String planet;
	private List<MapLocation> updatedStaticLocations;
	private List<MapLocation> updatedDynamicLocations;
	private List<MapLocation> updatedPersistLocations;
	private int staticLocVersion;
	private int dynamicLocVersion;
	private int persistentLocVersion;

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
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		planet					= getAscii(data);
		updatedStaticLocations = getList(data, MapLocation.class);
		updatedDynamicLocations = getList(data, MapLocation.class);
		updatedPersistLocations = getList(data, MapLocation.class);
		staticLocVersion		= getInt(data);
		dynamicLocVersion		= getInt(data);
		persistentLocVersion	= getInt(data);
	}

	@Override
	public ByteBuffer encode() {
		int size = planet.length() + 34;

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
		ByteBuffer data = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
		addShort(data, 8);
		addInt(data, CRC);

		addAscii(data, planet);

		addList(data, updatedStaticLocations);
		addList(data, updatedDynamicLocations);
		addList(data, updatedPersistLocations);

		addInt(data, staticLocVersion);
		addInt(data, dynamicLocVersion);
		addInt(data, persistentLocVersion);
		return data;
	}

	@Override
	public String toString() {
		return "[GetMapLocationsResponseMessage] " +
				"staticVersion=" + staticLocVersion + "dynamicVersion=" + dynamicLocVersion + "persistVersion=" + persistentLocVersion;
	}
}
