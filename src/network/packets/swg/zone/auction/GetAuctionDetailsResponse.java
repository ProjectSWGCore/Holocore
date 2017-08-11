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
package network.packets.swg.zone.auction;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class GetAuctionDetailsResponse extends SWGPacket {
	
	public static final int CRC = com.projectswg.common.data.CRC.getCrc("GetAuctionDetailsResponse");
	
	private long itemId;
	private Map <String, String> properties;
	private String itemName;
	
	public GetAuctionDetailsResponse() {
		this(0, new HashMap<String, String>(), "");
	}
	
	public GetAuctionDetailsResponse(long itemId, Map <String, String> properties, String itemName) {
		this.itemId = itemId;
		this.properties = properties;
		this.itemName = itemName;
	}
	
	public GetAuctionDetailsResponse(NetBuffer data) {
		decode(data);
	}
	
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		itemId = data.getLong();
		data.getInt();
		int count = data.getInt();
		for (int i = 0; i < count; i++) {
			String key = data.getAscii();
			String val = data.getUnicode();
			properties.put(key, val);
		}
		itemName = data.getAscii();
		data.getShort(); // 0
	}
	
	public NetBuffer encode() {
		int strSize = 0;
		for (Entry <String, String> e : properties.entrySet())
			strSize += 6 + e.getKey().length() + e.getValue().length()*2;
		NetBuffer data = NetBuffer.allocate(18 + strSize);
		data.addShort(9);
		data.addInt(CRC);
		data.addLong(itemId);
		data.addInt(properties.size());
		for (Entry <String, String> e : properties.entrySet()) {
			data.addAscii(e.getKey());
			data.addUnicode(e.getValue());
		}
		data.addAscii(itemName);
		data.addShort(0);
		return data;
	}

}
