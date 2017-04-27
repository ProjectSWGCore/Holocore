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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

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
	
	public AttributeListMessage(NetBuffer data) {
		decode(data);
	}
	
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		objectId = data.getLong();
		data.getAscii(); // static item name
		int count = data.getInt();
		for (int i = 0; i < count; i++) {
			String name = data.getAscii();
			String attr = data.getUnicode();
			attributes.put(name, attr);
		}
		data.getInt();
	}
	
	public NetBuffer encode() {
		int size = 0;
		for (Entry <String, String> e : attributes.entrySet()) {
			size += 6 + e.getKey().length() + (e.getValue().length() * 2);
		}
		NetBuffer data = NetBuffer.allocate(24 + size);
		data.addShort(3);
		data.addInt(CRC);
		data.addLong(objectId);
		data.addShort(0);
		data.addInt(attributes.size());
		for (Entry <String, String> e : attributes.entrySet()) {
			data.addAscii(e.getKey());
			data.addUnicode(e.getValue());
		}
		data.addInt(0);
		return data;
	}
	
}

