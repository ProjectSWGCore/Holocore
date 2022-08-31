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
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.nio.IntBuffer;
import java.util.BitSet;

public class TestSWGFlag extends TestRunnerNoIntents {
	
	@Test
	public void testFlagAccuracy() {
		SWGFlag flag = new SWGFlag(3, 16);
		flag.set(1);
		flag.set(4);
		flag.set(32);
		flag.set(64);
		flag.set(96);
		int [] ints = flag.toList();
		assertEquals(4, ints.length);
		assertEquals((1<<4)+(1<<1), ints[0]);
		assertEquals(1, ints[1]);
		assertEquals(1, ints[2]);
		assertEquals(1, ints[3]);
		SWGFlag decoded = new SWGFlag(3, 16);
		decoded.decode(NetBuffer.wrap(flag.encode()));
		assertArrayEquals(flag.encode(), decoded.encode());
	}
	
	@Test
	public void testFlagSize() {
		byte [] encoded;
		SWGFlag flag = new SWGFlag(3, 16);
		
		encoded = flag.encode();
		assertEquals(4, encoded.length);
		
		flag.clear();
		flag.set(0);
		encoded = flag.encode();
		assertEquals(8, encoded.length);
		assertEquals(1, encoded[0]);
		assertEquals(1, encoded[4]);
		
		flag.clear();
		flag.set(1);
		encoded = flag.encode();
		assertEquals(8, encoded.length);
		assertEquals(1, encoded[0]);
		assertEquals(2, encoded[4]);
		
		flag.clear();
		flag.set(7);
		encoded = flag.encode();
		assertEquals(8, encoded.length);
		assertEquals(1, encoded[0]);
		assertEquals((1 << 7), encoded[4] & 0xFF);
		
		flag.clear();
		flag.set(8);
		encoded = flag.encode();
		assertEquals(8, encoded.length);
		assertEquals(1, encoded[0]);
		assertEquals(0, encoded[4]);
		assertEquals(1, encoded[5]);
	}
	
}
