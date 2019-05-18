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

import java.util.*;
import java.util.stream.Collectors;

public class TestSWGList extends TestRunnerNoIntents {
	
	@Test
	public void testAdd() {
		int size = 24;
		String[] strings = new String[size];
		for (int i = 0; i < size; i++) {
			strings[i] = "test" + i;
		}
		
		SWGList<String> swgList = SWGList.Companion.createAsciiList(3, 6);
		swgList.addAll(List.of(strings));
		
		Assert.assertArrayEquals(strings, swgList.toArray());
		
		List<String> list = new ArrayList<>();
		Collections.addAll(list, strings);
		
		Assert.assertArrayEquals(swgList.toArray(), list.toArray());
	}
	
	@Test
	public void testIteratorUpdate() {
		int size = 24;
		List<String> strings = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			strings.add("test" + i);
		}
		
		SWGList<String> swgList = SWGList.Companion.createAsciiList(3, 6);
		swgList.addAll(strings);
		
		Assert.assertEquals(size, swgList.getUpdateCount());
		Assert.assertEquals(strings, swgList);
		for (ListIterator<String> it = swgList.listIterator(); it.hasNext(); ) {
			String str = it.next();
			it.set(str.replace('t', 'j'));
		}
		Assert.assertEquals(strings.stream().map(str -> str.replace('t', 'j')).collect(Collectors.toList()), swgList);
		Assert.assertEquals(size*2, swgList.getUpdateCount());
	}
	
	@Test
	public void testEncode() {
		int size = 24;
		NetBuffer expected = NetBuffer.allocate(8 + 2*size + 5*10 + 6*(size-10));
		expected.addInt(size);
		expected.addInt(size);
		String[] strings = new String[size];
		for (int i = 0; i < size; i++) {
			strings[i] = "test" + i;
			expected.addAscii(strings[i]);
		}
		
		SWGList<String> swgList = SWGList.Companion.createAsciiList(3, 6);
		Collections.addAll(swgList, strings);
		byte [] encoded = swgList.encode();
		Assert.assertArrayEquals(expected.array(), encoded);
		Assert.assertEquals(encoded.length, swgList.getLength());
		
		SWGList<String> decodedList = SWGList.Companion.createAsciiList(3, 6);
		decodedList.decode(NetBuffer.wrap(encoded));
		Assert.assertEquals(swgList, decodedList);
	}
}
