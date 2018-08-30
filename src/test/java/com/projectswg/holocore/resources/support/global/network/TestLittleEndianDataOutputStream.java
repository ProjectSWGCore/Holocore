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

import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TestLittleEndianDataOutputStream extends TestRunnerNoIntents {
	
	@Test
	public void test() throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(27).order(ByteOrder.LITTLE_ENDIAN);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LittleEndianDataOutputStream le = new LittleEndianDataOutputStream(baos);
		bb.put((byte) 15);			// 0
		bb.putShort((short) 1024);	// 1
		bb.putInt(1024);			// 3
		bb.putLong(1024);			// 7
		bb.putFloat(1.25f);			// 11
		bb.putDouble(1.75);			// 15
		le.write(15);
		le.writeShort(1024);
		le.writeInt(1024);
		le.writeLong(1024);
		le.writeFloat(1.25f);
		le.writeDouble(1.75);
		Assert.assertArrayEquals(bb.array(), baos.toByteArray());
		le.close();
	}
	
}
