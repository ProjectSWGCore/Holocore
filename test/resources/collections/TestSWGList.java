/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.projectswg.common.encoding.StringType;

/**
 * Created by Waverunner on 6/7/2015
 */
public class TestSWGList {

	@Test
	public void testAdd() {
		int size = 24;
		String[] strings = new String[24];
		for (int i = 0; i < size; i++) {
			strings[i] = "test" + String.valueOf(i);
		}

		SWGList<String> swgList = new SWGList<>(3, 6, StringType.ASCII);
		Collections.addAll(swgList, strings);

		Assert.assertArrayEquals(strings, swgList.toArray());

		List<String> list = new ArrayList<>();
		Collections.addAll(list, strings);

		Assert.assertArrayEquals(swgList.toArray(), list.toArray());
	}

	@Test
	public void testSet() {

	}

	@Test
	public void testRemove() {

	}

	@Test
	public void testEncode() {
		int size = 24;
		String[] strings = new String[24];
		for (int i = 0; i < size; i++) {
			strings[i] = "test" + String.valueOf(i);
		}

		SWGList<String> swgList = new SWGList<>(3, 6, StringType.ASCII);
		Collections.addAll(swgList, strings);
		swgList.encode();

	}
}
