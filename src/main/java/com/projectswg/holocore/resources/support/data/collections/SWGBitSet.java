/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.data.collections;

import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.BitSet;

public class SWGBitSet extends BitSet implements Encodable {
	
	private static final long serialVersionUID = 1L;
	
	private int view;
	private int updateType;
	
	public SWGBitSet() {
		super(128);
	}
	
	public SWGBitSet(int view, int updateType) {
		super(128); // Seems to be the default size for the bitmask sets in SWGPackets
		this.view = view;
		this.updateType = updateType;
	}
	
	@Override
	public byte[] encode() {
		byte[] bytes = toByteArray();
		NetBuffer buffer = NetBuffer.allocate(8 + bytes.length);
		buffer.addInt(bytes.length);
		buffer.addInt(super.length());
		buffer.addRawArray(bytes);
		return buffer.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		int len = data.getInt();
		data.getInt();
		byte [] bytes = data.getArray(len);
		clear();
		or(BitSet.valueOf(bytes));
	}
	
	@Override
	public int getLength() {
		return 8 + (super.length()+7) / 8;
	}
	
	public void read(byte[] bytes) {
		clear();

		if (bytes != null) {
			xor(valueOf(bytes));
		}
	}

	public void sendDeltaMessage(SWGObject target) {
		target.sendDelta(view, updateType, encode());
	}
	
}
