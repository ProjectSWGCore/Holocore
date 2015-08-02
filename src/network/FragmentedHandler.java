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
package network;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import network.packets.soe.Fragmented;

public class FragmentedHandler {
	
	private List<Fragmented> fragPackets;
	private int fragSize;
	
	public FragmentedHandler() {
		fragPackets = new ArrayList<Fragmented>();
		fragSize = 0;
	}
	
	public void reset() {
		fragPackets.clear();
		fragSize = 0;
	}
	
	public byte [] onReceived(Fragmented f) {
		synchronized (fragPackets) {
			if (insertIfNew(f) && getBufferedSize() == fragSize) {
				byte [] data = new byte[fragSize];
				int offset = 0;
				for (Fragmented frag : fragPackets) {
					offset = spliceFragmentedIntoBuffer(frag, data, offset);
				}
				updateMetadata();
				return data;
			}
			return new byte[0];
		}
	}
	
	private boolean insertIfNew(Fragmented f) {
		synchronized (fragPackets) {
			if (!fragPackets.contains(f)) {
				fragPackets.add(f);
				updateMetadata();
				return true;
			}
		}
		return false;
	}
	
	private int spliceFragmentedIntoBuffer(Fragmented f, byte [] data, int offset) {
		byte [] fData = f.encode().array();
		int header = offset==0?8:4;
		System.arraycopy(fData, header, data, offset, fData.length-header);
		return offset + fData.length-header;
	}
	
	private void updateMetadata() {
		synchronized (fragPackets) {
			if (fragPackets.isEmpty())
				fragSize = 0;
			else
				fragSize = fragPackets.get(0).encode().order(ByteOrder.BIG_ENDIAN).getInt(4);
		}
	}
	
	private int getBufferedSize() {
		int curSize = 0;
		int i = 0;
		short prevSeq = (short) (fragPackets.get(0).getSequence()-1);
		for (Fragmented frag : fragPackets) {
			// Update previous sequence and verify all in-order
			if (prevSeq+1 != frag.getSequence())
				break;
			prevSeq = frag.getSequence();
			// Update current size
			curSize += (i == 0) ? frag.encode().array().length-8 : frag.encode().array().length-4;
			i++;
			if (curSize >= fragSize)
				break;
		}
		return curSize;
	}
	
}
