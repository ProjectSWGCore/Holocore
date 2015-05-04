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
import java.util.Random;

import network.packets.soe.Fragmented;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestFragmented {
	
	@Test
	public void testFragmented() {
		Random r = new Random();
		byte [] randomData = new byte[1024];
		r.nextBytes(randomData);
		test(randomData);
	}
	
	@Test
	public void testFragmentedSmall() {
		Random r = new Random();
		byte [] randomData = new byte[100];
		r.nextBytes(randomData);
		test(randomData);
	}
	
	@Test
	public void testFragmentedEverySize() {
		for (int i = 0; i < 496*4; i++) {
			byte [] junkData = new byte[i];
			Fragmented main = new Fragmented();
			main.setPacket(ByteBuffer.wrap(junkData));
			try {
				main.encode(5);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Failed to encode at fragmented size " + i);
			}
		}
	}
	
	private void test(byte [] data) {
		FragmentedChannelA fragCore2 = new FragmentedChannelA();
		FragmentedChannelA [] fragsCore2 = fragCore2.create(data);
		
		Fragmented fragCore3 = new Fragmented();
		fragCore3.setPacket(ByteBuffer.wrap(data));
		Fragmented [] fragsCore3 = fragCore3.encode(5);
		
		Assert.assertEquals(fragsCore2.length, fragsCore3.length);
		for (int i = 0; i < fragsCore2.length; i++) {
			fragsCore2[i].setSequence((short) (i + 5));
			byte [] c2 = fragsCore2[i].serialize().array();
			byte [] c3 = fragsCore3[i].encode().array();
			Assert.assertArrayEquals(c2, c3);
		}
	}
	
}
