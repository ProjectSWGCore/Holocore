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

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestEncryption {
	
	@Test
	public void testEncryptionSmall() {
		Random r = new Random();
		int crc = r.nextInt();
		byte [] testInput = new byte[10];
		r.nextBytes(testInput);
		testInput[0] = 0;
		testInput[1] = 9;
		byte [] encoded = Encryption.encode(testInput, crc);
		byte [] decoded = Encryption.decode(encoded, crc);
		Assert.assertArrayEquals(testInput, decoded);
	}
	
	@Test
	public void testEncryptionLarge() {
		Random r = new Random();
		int crc = r.nextInt();
		byte [] testInput = new byte[256];
		r.nextBytes(testInput);
		testInput[0] = 0;
		testInput[1] = 9;
		byte [] encoded = Encryption.encode(testInput, crc);
		byte [] decoded = Encryption.decode(encoded, crc);
		Assert.assertArrayEquals(testInput, decoded);
	}
	
//	@Test
//	public void testEncryptionCorrectLarge() {
//		Random r = new Random();
//		int crc = r.nextInt();
//		byte [] testInput = new byte[256];
//		r.nextBytes(testInput);
//		testInput[0] = 0;
//		testInput[1] = 9;
//		MessageCRC cr = new MessageCRC();
//		MessageEncryption e = new MessageEncryption();
//		MessageCompression co = new MessageCompression();
//		byte [] encoded = Encryption.encode(testInput, crc);
//		byte [] expected = cr.append(e.encrypt(co.compress(testInput), crc), crc);
//		Assert.assertArrayEquals(expected, encoded);
//	}
	
//	@Test
//	public void testEncoding() {
//		Random r = new Random();
//		int crc = r.nextInt();
//		MessageCRC cr = new MessageCRC();
//		MessageEncryption e = new MessageEncryption();
//		MessageCompression co = new MessageCompression();
//		for (int size = 6; size < 496; size++) {
//			byte [] testInput = new byte[size];
//			r.nextBytes(testInput);
//			testInput[0] = 0;
////			testInput[1] = 9;
//			byte [] encoded = Encryption.encode(testInput, crc);
//			byte [] expected = cr.append(e.encrypt(co.compress(testInput), crc), crc);
//			Assert.assertArrayEquals(expected, encoded);
//		}
//	}
	
}
