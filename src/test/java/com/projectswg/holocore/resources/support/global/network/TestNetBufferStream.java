/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/

package com.projectswg.holocore.resources.support.global.network;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TestNetBufferStream extends TestRunnerNoIntents {
	
	@Test
	public void testExpansionSingle() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(5);
		}
	}
	
	@Test
	public void testExpansionBulk() {
		byte [] data = new byte[1024];
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(data);
		}
	}
	
	@Test
	public void testWriteReadString() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(generateTestString(i));
			for (int i = 0; i < 10; i++)
				Assert.assertEquals(getTestString(i), stream.getAscii());
		}
	}
	
	@Test
	public void testWriteReadCompactString() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(generateTestString(i));
			for (int i = 0; i < 10; i++) {
				Assert.assertEquals(getTestString(i), stream.getAscii());
				if (i == 4) {
					int rem = stream.remaining();
					stream.compact();
					Assert.assertEquals(0, stream.position());
					Assert.assertEquals(rem, stream.remaining());
				}
			}
		}
	}
	
	@Test
	public void testWriteReadInterleave() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++) {
				stream.write(generateTestString(i));
				if (i % 2 == 1) {
					Assert.assertEquals(getTestString(i-1), stream.getAscii());
					Assert.assertEquals(getTestString(i), stream.getAscii());
				}
			}
			Assert.assertEquals(0, stream.remaining());
		}
	}
	
	@Test
	public void testWriteReadCompactInterleave() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++) {
				stream.write(generateTestString(i));
				if (i % 2 == 1) {
					Assert.assertEquals(getTestString(i-1), stream.getAscii());
					Assert.assertEquals(getTestString(i), stream.getAscii());
					int rem = stream.remaining();
					stream.compact();
					Assert.assertEquals(0, stream.position());
					Assert.assertEquals(rem, stream.remaining());
				}
			}
			Assert.assertEquals(0, stream.remaining());
		}
	}
	
	@Test
	public void testRewind() {
		try (NetBufferStream stream = new NetBufferStream(4)) {
			for (int i = 0; i < 10; i++)
				stream.write(generateTestString(i));
			for (int i = 0; i < 10; i++) {
				Assert.assertEquals(getTestString(i), stream.getAscii());
				if (i == 4)
					stream.mark();
			}
			Assert.assertEquals(0, stream.remaining());
			stream.rewind();
			Assert.assertEquals(5*(2+getTestString(0).length()), stream.remaining());
			for (int i = 5; i < 10; i++)
				Assert.assertEquals(getTestString(i), stream.getAscii());
		}
	}
	
	private byte [] generateTestString(int num) {
		String test = getTestString(num);
		byte [] data = new byte[2 + test.length()];
		data[0] = (byte) test.length();
		System.arraycopy(test.getBytes(StandardCharsets.US_ASCII), 0, data, 2, data[0]);
		return data;
	}
	
	private String getTestString(int num) {
		return "Hello World " + num + "!";
	}
	
}
