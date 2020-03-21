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
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.Arrays;
import java.util.BitSet;

public class SWGFlag extends BitSet implements Encodable, Persistable {
	
	private static final long serialVersionUID = 2L;
	
	private final int view;
	private final int updateType;
	
	/**
	 * Creates a new {@link SWGFlag} for the defined baseline with the given view and update. Note
	 * that this is an extension of {@link BitSet}
	 * 
	 * @param view The baseline number this BitSet resides in
	 * @param updateType The update variable used for sending a delta, it's the operand count that
	 *            this BitSet resides at within the baseline
	 */
	public SWGFlag(int view, int updateType) {
		super(128); // Seems to be the default size for the bitmask sets in SWGPackets
		this.view = view;
		this.updateType = updateType;
	}
	
	@Override
	public byte[] encode() {
		byte [] encoded = toByteArray();
		int length = (int) Math.ceil(encoded.length / 4.0);
		
		NetBuffer buffer = NetBuffer.allocate((length + 1) * 4);
		buffer.addInt(length);
		buffer.addRawArray(encoded);
		return buffer.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		int len = data.getInt();
		byte [] encoded = data.getArray(len * 4);
		
		clear();
		xor(BitSet.valueOf(encoded));
	}
	
	@Override
	public int getLength() {
		return 4 + (int) Math.ceil(super.size()/32.0);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addArray(toByteArray());
	}
	
	@Override
	public void read(NetBufferStream stream) {
		clear();
		xor(valueOf(stream.getArray()));
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SWGFlag))
			return super.equals(o);
		return Arrays.equals(toList(), ((SWGFlag) o).toList());
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(toList());
	}
	
	public void sendDeltaMessage(SWGObject target) {
		target.sendDelta(view, updateType, encode());
	}
	
	public int[] toList() {
		int[] integers = new int[(int) Math.ceil(size()/32.0)];
		
		for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i+1)) {
			integers[i / 32] |= (1 << (i % 32));
		}
		
		return integers;
	}
}
