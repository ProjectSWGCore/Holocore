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
package network.encryption;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Encryption {
	
	public static String md5(String plaintext) {
		return MD5.digest(plaintext);
	}
	
	public static byte [] encode(byte [] input, int crc) {
		try {
			return assembleMessage(input, crc);
		} catch (Throwable t) {
			t.printStackTrace();
			return new byte[0];
		}
	}
	
	public static byte [] decode(byte [] input, int crc) {
		try {
			return disassemble(input, crc);
		} catch (Throwable t) {
			t.printStackTrace();
			return new byte[0];
		}
	}
	
	private static byte [] disassemble(byte [] data, int crcSeed) {
		return decompress(encrypt(EncryptionCRC.validate(data, crcSeed), crcSeed, false));
	}
	
	private static byte [] assembleMessage(byte [] data, int crc) {
		if (data.length < 2)
			return data;
		return EncryptionCRC.append(encrypt(compress(data), crc, true), crc);
	}
	
	public static byte [] encrypt(byte [] data, int iCrc, boolean encrypt) {
		if (data.length < 2)
			return data;
		byte [] encrypted = new byte[data.length];
		int start = 2;
		encrypted[0] = data[0];
		if (data[0] != 0)
			start = 1;
		else
			encrypted[1] = data[1];
		if (data.length < 6) {
			for (int i = start; i < data.length; i++)
				encrypted[i] = (byte) (data[i] ^ iCrc);
			return encrypted;
		}
		int lastBytes = start+(int)((data.length-start)/4)*4;
		byte [] crc = new byte[] {(byte)(iCrc), (byte)(iCrc >>> 8),(byte)(iCrc >>> 16), (byte)(iCrc >>> 24)};
		for (int i = start; i < lastBytes; i++) {
			encrypted[i] = (byte) (data[i] ^ crc[(i-start)%4]);
			crc[(i-start)%4] = encrypt ? encrypted[i] : data[i];
		}
		if (lastBytes >= 4) { // Avoids an Index Out of Bounds Exception
			for (int i = lastBytes; i < data.length; i++) { // Remaining 0-3 bytes that won't fit w/ the crc
				byte lastCrc = encrypt ? encrypted[lastBytes-4] : data[lastBytes-4];
				encrypted[i] = (byte) (data[i] ^ lastCrc);
			}
		}
		return encrypted;
	}
	
	public static byte [] compress(byte [] data) {
		if (data.length >= 200) {
			byte [] result = new byte[512];
			Deflater compressor = new Deflater();
			compressor.setInput(data, 2, data.length - 2);
			compressor.finish();
			int length = 0;
			try {
				length = compressor.deflate(result);
			} finally {
				compressor.end();
			}
			if (length < data.length) {
				ByteBuffer bb = ByteBuffer.allocate(length+3);
				bb.put(data[0]).put(data[1]);
				bb.put(result, 0, length);
				bb.put((byte) 1);
				return bb.array();
			}
		}
		return ByteBuffer.allocate(data.length+1).put(data).put((byte) 0).array();
	}
	
	private static byte [] decompress(byte [] data) {
		if (data == null || data.length == 0)
			return data;
		else if (data[data.length - 1] == 0)
			return ByteBuffer.allocate(data.length - 1).put(data, 0, data.length - 1).array();
		else if (data[data.length - 1] != 1)
			return new byte[0];
		
		int startingIndex = data[0] > 0 ? 1 : 2;
		byte [] result = new byte[512];
		int length = inflate(startingIndex, data, result);
		if (length == -1)
			return data;
		
		byte [] ret = new byte[length+startingIndex];
		ret[0] = data[0];
		if (startingIndex > 1)
			System.arraycopy(data, 1, ret, 1, startingIndex-1);
		System.arraycopy(result, 0, ret, startingIndex, length);
		return ret;
	}
	
	private static int inflate(int startingIndex, byte [] data, byte [] result) {
		if (data == null || data.length - startingIndex < 0)
			return -1;
		Inflater decompressor = new Inflater();
		decompressor.setInput(data, startingIndex, data.length - startingIndex);
		
		try {
			int length = decompressor.inflate(result);
			return length;
		} catch (DataFormatException e) {
			System.err.println("Failed to decompress packet. "+e.getClass().getSimpleName()+" Message: " + e.getMessage());
			return -1;
		} finally {
			decompressor.end();
		}
	}
	
}
