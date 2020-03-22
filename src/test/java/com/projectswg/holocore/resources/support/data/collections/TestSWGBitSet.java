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

import com.projectswg.common.network.NetBuffer;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

public class TestSWGBitSet extends TestRunnerNoIntents {
	
	@Test
	public void testFlag() {
		SWGBitSet flag = new SWGBitSet(3, 16);
		flag.set(1);
		flag.set(4); // 1 byte
		flag.set(8); // 2 bytes
		flag.set(16); // 3 bytes
		flag.set(32); // 5 bytes
		byte [] actual = flag.encode();
		byte [] expected = new byte[] {
			5, 0, 0, 0,
			33, 0, 0, 0,
			(1<<4)+(1<<1), 1, 1, 0, 1
		};
		Assert.assertArrayEquals(expected, actual);
	}
	
	@Test
	public void testDecode() {
		SWGBitSet flag = new SWGBitSet(3, 16);
		
		byte [] data = new byte[] {
				5, 0, 0, 0,
				33, 0, 0, 0,
				(1<<4)+(1<<1), 1, 1, 0, 1
		};
		
		NetBuffer buffer = NetBuffer.wrap(data);
		
		flag.decode(buffer);
		
		Assert.assertTrue("Flag 1 should be set", flag.get(1));
		Assert.assertTrue("Flag 4 should be set", flag.get(4));
		Assert.assertTrue("Flag 8 should be set", flag.get(8));
		Assert.assertTrue("Flag 16 should be set", flag.get(16));
		Assert.assertTrue("Flag 32 should be set", flag.get(32));
		Assert.assertFalse("Flag 64 should be not set", flag.get(64));
	}
	
	@Test
	public void testGetLength() {
		SWGBitSet flag = new SWGBitSet(3, 16);
		
		flag.set(4);
		flag.set(8);
		
		Assert.assertEquals("Two flags should fill 10 bytes", 10, flag.getLength());
	}
	
}
