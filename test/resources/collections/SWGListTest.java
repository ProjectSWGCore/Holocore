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

import network.packets.swg.zone.baselines.Baseline;
import org.junit.Assert;
import org.junit.Test;
import utilities.Encoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Waverunner on 6/7/2015
 */
public class SWGListTest {

	@Test
	public void testAdd() throws Exception {
		int size = 24;
		String[] strings = new String[24];
		for (int i = 0; i < size; i++) {
			strings[i] = "test" + String.valueOf(i);
		}

		SWGList<String> swgList = new SWGList<>(Baseline.BaselineType.PLAY, 3, 6, Encoder.StringType.ASCII);
		long start = System.nanoTime();
		Collections.addAll(swgList, strings);
		long end = System.nanoTime();

		Assert.assertArrayEquals(strings, swgList.toArray());
		System.out.println("SWGList: Took "+ (end-start)/1E6 + "ms to add " + size + " elements");

		List<String> list = new ArrayList<>();
		long startList = System.nanoTime();
		Collections.addAll(list, strings);
		long endList = System.nanoTime();

		Assert.assertArrayEquals(swgList.toArray(), list.toArray());
		System.out.println("ArrayList: Took "+ (endList-startList)/1E6 + "ms to add " + size + " elements");
	}

	@Test
	public void testSet() throws Exception {

	}

	@Test
	public void testRemove() throws Exception {

	}

	@Test
	public void testEncode() throws Exception {
		int size = 24;
		String[] strings = new String[24];
		for (int i = 0; i < size; i++) {
			strings[i] = "test" + String.valueOf(i);
		}

		SWGList<String> swgList = new SWGList<>(Baseline.BaselineType.PLAY, 3, 6, Encoder.StringType.ASCII);
		Collections.addAll(swgList, strings);

		long start = System.nanoTime();
		swgList.encode();
		long end = System.nanoTime();

		System.out.println("SWGList: Took " + (end-start)/1E6 + "ms to encode data");

	}
}