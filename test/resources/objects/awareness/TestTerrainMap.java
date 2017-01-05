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
package resources.objects.awareness;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import resources.Location;
import resources.Terrain;
import resources.objects.SWGObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.server_info.Log;
import services.objects.ClientBuildoutService;

@RunWith(JUnit4.class)
public class TestTerrainMap {
	
	@Test
	public void testWithoutParent() {
		TerrainMap map = new TerrainMap(Terrain.TATOOINE);
		MapCallback callback = new MapCallback();
		map.setCallback(callback);
		try {
			map.start();
			GenericCreatureObject objA = new GenericCreatureObject(1);
			GenericCreatureObject objB = new GenericCreatureObject(2);
			map.moveWithinMap(objA, new Location(0, 0, 0, Terrain.TATOOINE)); // 0 - Within Range, 1 - Successful Move
			map.moveWithinMap(objB, new Location(5, 0, 5, Terrain.TATOOINE)); // 1 - Within Range, 1 - Successful Move
			awaitCallbacks(map, 1000);
			callback.testAssert(1, 0, 2, 0);
		} finally {
			map.stop();
		}
	}
	
	@Test
	public void testWithParent() {
		TerrainMap map = new TerrainMap(Terrain.TATOOINE);
		MapCallback callback = new MapCallback();
		map.setCallback(callback);
		try {
			map.start();
			GenericCreatureObject objA = new GenericCreatureObject(1);
			GenericCreatureObject objB = new GenericCreatureObject(2);
			CellObject cell = new CellObject(3);
			map.moveWithinMap(cell, new Location(5, 0, 5, Terrain.TATOOINE)); // 0 - Within Range, 1 - Successful Move
			map.moveWithinMap(objA, new Location(0, 0, 0, Terrain.TATOOINE)); // 1 - Within Range, 1 - Successful Move
			objB.setLocation(new Location(5, 0, 5, Terrain.TATOOINE));
			objB.moveToContainer(cell);
			awaitCallbacks(map, 1000);
			callback.testAssert(1, 0, 2, 0);
		} finally {
			map.stop();
		}
	}
	
	@Test
	public void testMultiChunk() {
		TerrainMap map = new TerrainMap(Terrain.TATOOINE);
		MapCallback callback = new MapCallback();
		map.setCallback(callback);
		try {
			map.start();
			GenericCreatureObject objA = new GenericCreatureObject(1);
			GenericCreatureObject objB = new GenericCreatureObject(2);
			objA.setPrefLoadRange(200);
			objB.setPrefLoadRange(200);
			map.moveWithinMap(objA, new Location(5, 0, 5, Terrain.TATOOINE)); // 0 - Within Range, 1 - Successful Move
			map.moveWithinMap(objB, new Location(-5, 0, -5, Terrain.TATOOINE)); // 1 - Within Range, 1 - Successful Move
			awaitCallbacks(map, 1000);
			callback.testAssert(1, 0, 2, 0);
		} finally {
			map.stop();
		}
	}
	
	@Test
	public void testTatooine() {
		ClientBuildoutService buildoutService = new ClientBuildoutService();
		Collection<SWGObject> allObjects = buildoutService.loadClientObjectsByArea(843).values(); // mos eisley's area id
		List<SWGObject> tatObjects = allObjects.stream().filter((obj) -> {
			return obj.getTerrain() == Terrain.TATOOINE && obj.getParent() == null;
		}).collect(Collectors.toList());
		TerrainMap map = new TerrainMap(Terrain.TATOOINE);
		List<SWGObject> withinRange = new Vector<>();
		AtomicBoolean onlyWithinRange = new AtomicBoolean(true);
		GenericCreatureObject creature = new GenericCreatureObject(1);
		MapCallback callback = new MapCallback() {
			public void onWithinRange(SWGObject obj, SWGObject inRange) {
				if (obj.equals(creature) && !withinRange.remove(inRange)) {
					Log.e(this, "Not in list: %s", inRange);
					onlyWithinRange.set(false);
				}
			}
		};
		map.setCallback(callback);
		try {
			map.start();
			creature.setName("testTatooine");
			Location creatureLocation = new Location(3500, 5, -4800, Terrain.TATOOINE);
			for (SWGObject obj : tatObjects) {
				double range = Math.min(1024*Math.sqrt(2), Math.max(obj.getLoadRange(), creature.getLoadRange()));
				if (isValidWithinRange(creature, obj, creatureLocation, range))
					withinRange.add(obj);
				map.moveWithinMap(obj, obj.getLocation());
			}
			map.moveWithinMap(creature, creatureLocation);
			awaitCallbacks(map, 1000);
			Assert.assertEquals(0, withinRange.size());
			Assert.assertTrue("TEST-ONLY-WITHIN-RANGE", onlyWithinRange.get());
		} finally {
			map.stop();
		}
	}
	
	private boolean isValidWithinRange(SWGObject obj, SWGObject inRange, Location objLocation, double range) {
		if (obj.equals(inRange))
			return false;
		if (inRange instanceof CreatureObject && ((CreatureObject) inRange).isLoggedOutPlayer())
			return false;
		if (!inRange.getWorldLocation().isWithinFlatDistance(objLocation, Math.max(range, inRange.getLoadRange())))
			return false;
		return true;
	}
	
	private void awaitCallbacks(TerrainMap map, long timeout) {
		try {
			while (!map.isCallbacksDone() && timeout > 0) {
				Thread.sleep(1);
				timeout--;
			}
		} catch (InterruptedException e) {
			Log.e(this, e);
		}
	}
	
}
