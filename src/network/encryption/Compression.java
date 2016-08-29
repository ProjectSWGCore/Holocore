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
package network.encryption;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

public class Compression {
	
	private static final LZ4Compressor COMPRESSOR = LZ4Factory.safeInstance().highCompressor();
	private static final LZ4SafeDecompressor DECOMPRESSOR = LZ4Factory.safeInstance().safeDecompressor();
	
	public static int getMaxCompressedLength(int len) {
		return COMPRESSOR.maxCompressedLength(len);
	}
	
	public static int compress(byte [] data, byte [] buffer) {
		return COMPRESSOR.compress(data, buffer);
	}
	
	public static byte [] decompress(byte [] data) {
		return decompress(data, data.length * 10);
	}
	
	public static byte [] decompress(byte [] data, int bufferSize) {
		byte [] restored = new byte[bufferSize];
		int length = DECOMPRESSOR.decompress(data, restored);
		if (length == bufferSize)
			return restored;
		byte [] ret = new byte[length];
		System.arraycopy(restored, 0, ret, 0, length);
		return ret;
	}
	
}
