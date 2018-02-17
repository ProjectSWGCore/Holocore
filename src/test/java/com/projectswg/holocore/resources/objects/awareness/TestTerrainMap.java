/************************************************************************************
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
package com.projectswg.holocore.resources.objects.awareness;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Log;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.test_resources.GenericCreatureObject;

@RunWith(JUnit4.class)
public class TestTerrainMap {
	
	@Test
	public void testWithoutParent() {
		MapCallback callback = new MapCallback();
		TerrainMap map = new TerrainMap(Terrain.TATOOINE, callback);
		try {
			map.start();
			GenericCreatureObject objA = new GenericCreatureObject(1);
			GenericCreatureObject objB = new GenericCreatureObject(2);
			objA.setPosition(Terrain.TATOOINE, 0, 0, 0);
			objB.setPosition(Terrain.TATOOINE, 5, 0, 5);
			map.moveWithinMap(objA); // 0 - Within Range, 1 - Successful Move
			map.moveWithinMap(objB); // 1 - Within Range, 1 - Successful Move
			callback.testAssert(1, 0, 2, 0);
		} finally {
			map.stop();
		}
	}
	
	@Test
	public void testWithParent() {
		MapCallback callback = new MapCallback();
		TerrainMap map = new TerrainMap(Terrain.TATOOINE, callback);
		try {
			map.start();
			GenericCreatureObject objA = new GenericCreatureObject(1);
			GenericCreatureObject objB = new GenericCreatureObject(2);
			CellObject cell = new CellObject(3);
			cell.setPosition(Terrain.TATOOINE, 5, 0, 5);
			objA.setPosition(Terrain.TATOOINE, 0, 0, 0);
			map.moveWithinMap(cell); // 0 - Within Range, 1 - Successful Move
			map.moveWithinMap(objA); // 1 - Within Range, 1 - Successful Move
			objB.setPosition(Terrain.TATOOINE, 5, 0, 5);
			objB.moveToContainer(cell);
			callback.testAssert(1, 0, 2, 0);
		} finally {
			map.stop();
		}
	}
	
	@Test
	public void testMultiChunk() {
		MapCallback callback = new MapCallback();
		TerrainMap map = new TerrainMap(Terrain.TATOOINE, callback);
		try {
			map.start();
			GenericCreatureObject objA = new GenericCreatureObject(1);
			GenericCreatureObject objB = new GenericCreatureObject(2);
			objA.setPrefLoadRange(200);
			objB.setPrefLoadRange(200);
			objA.setPosition(Terrain.TATOOINE, 5, 0, 5);
			objB.setPosition(Terrain.TATOOINE, -5, 0, -5);
			map.moveWithinMap(objA); // 0 - Within Range, 1 - Successful Move
			map.moveWithinMap(objB); // 1 - Within Range, 1 - Successful Move
			callback.testAssert(1, 0, 2, 0);
		} finally {
			map.stop();
		}
	}
	
	@Test
	public void testTatooine() {
		AtomicBoolean onlyWithinRange = new AtomicBoolean(true);
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setPosition(Terrain.TATOOINE, 3500, 5, -4800);
		List<SWGObject> withinRange = TestBuildoutObjectList.getInstance().getWithinRangeObjects(creature);
		
		TerrainMap map = new TerrainMap(Terrain.TATOOINE, new MapCallback() {
			@Override
			public void onWithinRange(SWGObject obj, SWGObject inRange) {
				if (obj.equals(creature) && !withinRange.remove(inRange)) {
					Log.e("Not in list: %s", inRange);
					onlyWithinRange.set(false);
				}
			}
		});
		try {
			map.start();
			for (SWGObject obj : getMosEisleyObjects()) {
				map.moveWithinMap(obj);
			}
			map.moveWithinMap(creature);
			Assert.assertEquals(0, withinRange.size());
			Assert.assertTrue("TEST-ONLY-WITHIN-RANGE", onlyWithinRange.get());
		} finally {
			map.stop();
		}
	}
	
	private static List<SWGObject> getMosEisleyObjects() {
		return TestBuildoutObjectList.getInstance().getMosEisleyObjects();
	}
	
}
