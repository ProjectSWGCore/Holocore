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
package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

import com.projectswg.common.network.NetBuffer;

public class ShowLootBox extends ObjectController {
	
	public static final int CRC = 0x04BC;
	
	private long[] items;

	public ShowLootBox(long objectId, long[] items) {
		super(objectId, CRC);
		this.items = items;
	}
	
	public ShowLootBox(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		
		int itemCount = data.getInt();
		items = new long[itemCount];
		
		for(int i = 0; i < itemCount; i++ ) {
			items[i] = data.getLong();
		}
	}
	
	@Override
	public ByteBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + Integer.BYTES + items.length * Long.BYTES);
		encodeHeader(data.getBuffer());
		
		data.addInt(items.length);
		
		for(long objectId : items) {
			data.addLong(objectId);
		}
		
		return data.getBuffer();
	}

}

