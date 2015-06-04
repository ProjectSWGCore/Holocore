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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import network.packets.swg.SWGPacket;
import services.map.MapLocation;

public class GetMapLocationsResponseMessage extends SWGPacket {
	public static final int CRC = 0x9F80464C;
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

	public ByteBuffer encode() {
		int size = planet.length() + 34;

		// Encode the map locations to byte arrays and retrieve sizes for buffer allocation
		List<byte[]> encodedStatic = null;
		List<byte[]> encodedDynamic = null;
		List<byte[]> encodedPersist = null;

		if (updatedStaticLocations != null) {
			encodedStatic = new ArrayList<>();
			for (MapLocation location : updatedStaticLocations) {
				byte[] data = location.encode();
				encodedStatic.add(data);
				size += data.length;
			}
		}

		if (updatedDynamicLocations != null) {
			encodedDynamic = new ArrayList<>();
			for (MapLocation location : updatedDynamicLocations) {
				byte[] data = location.encode();
				encodedDynamic.add(data);
				size += data.length;
			}
		}

		if (updatedPersistLocations != null) {
			encodedPersist = new ArrayList<>();
			for (MapLocation location : updatedPersistLocations) {
				byte[] data = location.encode();
				encodedPersist.add(data);
				size += data.length;
			}
		}

		// Create the packet
		ByteBuffer data = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
		addShort(data, 8);
		addInt(data, CRC);

		addAscii(data, planet);

		if (encodedStatic != null) {
			addInt(data, encodedStatic.size());
			for (byte[] bytes : encodedStatic) {
				data.put(bytes);
			}
		} else {
			addInt(data, 0);
		}

		if(encodedDynamic != null) {
			addInt(data, encodedDynamic.size());
			for (byte[] bytes : encodedDynamic) {
				data.put(bytes);
			}
		} else {
			addInt(data, 0);
		}

		if (encodedPersist != null) {
			addInt(data, encodedPersist.size());
			for (byte[] bytes : encodedPersist) {
				data.put(bytes);
			}
		} else {
			addInt(data, 0);
		}

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
