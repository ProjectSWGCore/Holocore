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
package resources.server_info;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import resources.server_info.ObjectDatabase.Traverser;

@RunWith(JUnit4.class)
public class TestObjectDatabase {
	
	private void cleanup(String filename) {
		File f = new File(filename);
		f.delete();
	}
	
	@Test
	public void testPutGet() {
		cleanup("test_odb.db");
		UncachedObjectDatabase<Integer> odb = new UncachedObjectDatabase<Integer>("test_odb.db");
		odb.put(500, 1024);
		Assert.assertEquals(1024, odb.get(500).intValue());
		odb.put("testString", 128);
		Assert.assertEquals(128, odb.get("testString").intValue());
	}
	
	@Test
	public void testClearCache() {
		cleanup("test_odb.db");
		UncachedObjectDatabase<Integer> odb = new UncachedObjectDatabase<Integer>("test_odb.db");
		odb.put(500, 1024);
		Assert.assertEquals(1024, odb.get(500).intValue());
		odb.clearCache();
		Assert.assertEquals(null, odb.get(500));
	}
	
	@Test
	public void testSaveLoad() {
		cleanup("test_odb.db");
		UncachedObjectDatabase<Integer> odb = new UncachedObjectDatabase<Integer>("test_odb.db");
		odb.put(500, 1024);
		Assert.assertEquals(1024, odb.get(500).intValue());
		odb.save();
		odb = new UncachedObjectDatabase<Integer>("test_odb.db");
		Assert.assertEquals(1024, odb.get(500).intValue());
	}
	
	@Test
	public void testSaveClearCache() {
		cleanup("test_odb.db");
		UncachedObjectDatabase<Integer> odb = new UncachedObjectDatabase<Integer>("test_odb.db");
		odb.put(500, 1024);
		Assert.assertEquals(1024, odb.get(500).intValue());
		odb.save();
		odb.clearCache();
		Assert.assertEquals(1024, odb.get(500).intValue());
		Assert.assertEquals(1024, odb.get(500).intValue());
	}
	
	@Test
	public void testCache() {
		cleanup("test_odb.db");
		UncachedObjectDatabase<Integer> odb = new UncachedObjectDatabase<Integer>("test_odb.db");
		odb.put(500, 1024);
		Assert.assertEquals(1024, odb.get(500).intValue());
		odb.save();
		odb.clearCache();
		odb.loadToCache();
		long start = System.nanoTime();
		Integer get = odb.get(500);
		long end = System.nanoTime();
		Assert.assertEquals(1024, get.intValue());
		Assert.assertTrue("Cached get() must have taken less than 0.01ms - time: " + (end-start)/1E6 + "ms", (end-start)/1E6 <= 0.01);
	}
	
	@Test
	public void testTraverser() {
		cleanup("test_odb.db");
		UncachedObjectDatabase<Integer> odb = new UncachedObjectDatabase<Integer>("test_odb.db");
		odb.put(1024, 301);
		odb.put(2048, 301);
		odb.put(4096, 301);
		final int [] traverseLoops = new int[] {0};
		odb.traverse(new Traverser<Integer>() {
			public void process(Integer i) {
				traverseLoops[0]++;
			}
		});
		Assert.assertEquals(3, traverseLoops[0]);
	}
	
}
