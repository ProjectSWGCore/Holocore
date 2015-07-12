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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class AttributeListMessage extends SWGPacket {
	public static final int CRC = getCrc("AttributeListMessage");
	
	private long objectId;
	private Map <String, String> attributes;
	
	public AttributeListMessage() {
		this(0, new HashMap<>());
	}
	
	public AttributeListMessage(long objectId, Map <String, String> attributes) {
		this.objectId = objectId;
		this.attributes = attributes;
	}
	
	public AttributeListMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objectId = getLong(data);
		getShort(data);
		int count = getInt(data);
		for (int i = 0; i < count; i++) {
			String name = getAscii(data);
			String attr = getUnicode(data);
			attributes.put(name, attr);
		}
		getInt(data);
	}
	
	public ByteBuffer encode() {
		int size = 0;
		for (Entry <String, String> e : attributes.entrySet()) {
			size += 6 + e.getKey().length() + (e.getValue().length() * 2);
		}
		ByteBuffer data = ByteBuffer.allocate(24 + size);
		addShort(data, 3);
		addInt  (data, CRC);
		addLong (data, objectId);
		addShort(data, 0);
		addInt  (data, attributes.size());
		for (Entry <String, String> e : attributes.entrySet()) {
			addAscii(data, e.getKey());
			addUnicode(data, e.getValue());
		}
		addInt  (data, 0);
		return data;
	}
	
}

