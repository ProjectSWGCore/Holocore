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
package utilities;

import java.nio.ByteBuffer;


public class ByteUtilities {
	
	private static final ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
	private static final char [] HEX = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public static final String getHexString(byte [] bytes) {
		StringBuffer ret = new StringBuffer(bytes.length*2+(bytes.length>0?bytes.length-1:0));
		for (byte b : bytes) {
			ret.append(getHexString(b) + ' ');
		}
		return new String(ret);
	}
	
	public static final String getHexString(Byte [] bytes) {
		StringBuffer ret = new StringBuffer(bytes.length*2+(bytes.length>0?bytes.length-1:0));
		for (byte b : bytes) {
			ret.append(getHexString(b) + ' ');
		}
		return new String(ret);
	}
	
	public static final String getHexString(byte b) {
		return String.valueOf(HEX[(b&0xFF) >>> 4]) + HEX[b & 0x0F];
	}
	
	public static final byte [] longToBytes(long l) {
		byte [] b = new byte[Long.SIZE];
		synchronized (buffer) {
			buffer.putLong(0, l);
			System.arraycopy(buffer.array(), 0, b, 0, Long.SIZE);
		}
		return b;
	}
	
	public static final long bytesToLong(byte [] a) {
		return bytesToLong(a, 0);
	}
	
	public static final long bytesToLong(byte [] a, int offset) {
		long l = 0;
		synchronized (buffer) {
			for (int i = 0; i < Long.SIZE; i++) {
				if (i < a.length)
					buffer.put(i, a[i+offset]);
				else
					buffer.put(i, (byte)0);
			}
			l = buffer.getLong(0);
		}
		return l;
	}
	
	public static String nextString(ByteBuffer data) {
		byte [] bData = data.array();
		StringBuilder str = new StringBuilder();
		for (int i = data.position(); i < bData.length && bData[i] >= ' ' && bData[i] <= '~'; i++)
			str.append((char) data.get());
		return str.toString();
	}
}
