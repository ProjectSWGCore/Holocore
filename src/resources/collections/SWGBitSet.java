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
package resources.collections;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

import resources.objects.SWGObject;

public class SWGBitSet extends BitSet implements Encodable, Persistable {
	
	private static final long serialVersionUID = 1L;
	
	private int view;
	private int updateType;
	
	public SWGBitSet(int view, int updateType) {
		super(128); // Seems to be the default size for the bitmask sets in packets
		this.view = view;
		this.updateType = updateType;
	}
	
	@Override
	public byte[] encode() {
		BitSet b = ((BitSet) this);
		byte[] bytes = toByteArray();
		ByteBuffer buffer = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(bytes.length);
		buffer.putInt(b.length());
		buffer.put(bytes);
		return buffer.array();
	}
	
	@Override
	public void decode(ByteBuffer data) {
		// TODO: Decode method for SWGBitSet
		throw new UnsupportedOperationException("Unable to decode bitset!");
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
	
	public void sendDeltaMessage(SWGObject target) {
		target.sendDelta(view, updateType, encode());
	}
	
}
