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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Log;

import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.creature.CreatureObject;
import services.objects.ClientBuildoutService;
import test_resources.GenericCreatureObject;

@RunWith(JUnit4.class)
public class TestAwarenessHandler {
	
	private static final Location CREATURE_LOCATION = new Location(3618, 5, -4801, Terrain.TATOOINE);
	private static final Location CREATURE2_LOCATION = new Location(3650, 5, -4800, Terrain.TATOOINE);
	
	private static final List<SWGObject> EISLEY_OBJECTS = new ArrayList<>(2524);
	private static final List<SWGObject> WITHIN_RANGE = new ArrayList<>(537);
	
	private static final GenericCreatureObject CREATURE2 = new GenericCreatureObject(2);
	private static final GenericCreatureObject CREATURE3 = new GenericCreatureObject(3);
	
	@BeforeClass
	public static void initTatooine() {
		ClientBuildoutService buildoutService = new ClientBuildoutService();
		buildoutService.initialize();
		double loadDistance = 1024*1.414*2; // 1024 * sqrt(2)
		for (SWGObject obj : buildoutService.loadClientObjectsByArea(843).values()) { // mos eisley's area id
			initObject(obj, loadDistance);
		}
		for (SWGObject obj : buildoutService.loadClientObjectsByArea(-60).values()) { // general tatooine's area id
			initObject(obj, loadDistance);
		}
		buildoutService.terminate();
		CREATURE2.setLocation(CREATURE2_LOCATION);
		initObject(CREATURE2, loadDistance);
		CREATURE3.setLocation(CREATURE2_LOCATION);
		CREATURE3.setHasOwner(false);
		initObject(CREATURE3, loadDistance);
	}
	
	private static void initObject(SWGObject obj, double loadDistance) {
		if (obj.getParent() == null && CREATURE_LOCATION.isWithinFlatDistance(obj.getLocation(), loadDistance)) {
			EISLEY_OBJECTS.add(obj);
			if (isValidWithinRange(obj, CREATURE_LOCATION, Math.max(obj.getLoadRange(), 200)))
				WITHIN_RANGE.add(obj);
		}
	}
	
	private static boolean isValidWithinRange(SWGObject inRange, Location objLocation, double range) {
		if (inRange instanceof CreatureObject && ((CreatureObject) inRange).isLoggedOutPlayer())
			return false;
		if (!inRange.getWorldLocation().isWithinFlatDistance(objLocation, Math.max(range, inRange.getLoadRange())))
			return false;
		return true;
	}
	
	@Test
	public void testMoveSameAwareness() throws InterruptedException {
		MapCallbackRealistic callback = new MapCallbackRealistic();
		try (AwarenessHandler awareness = new AwarenessHandler(callback)) {
			GenericCreatureObject creature = new GenericCreatureObject(1);
			initAwareness(awareness, callback);
			awareness.moveObject(creature, CREATURE_LOCATION);
			awaitCallbacks(awareness);
			callback.testAssert(WITHIN_RANGE.size(), 0, 1, 0);
			callback.set(0, 0, 0, 0);
			awareness.moveObject(creature, CREATURE_LOCATION);
			awaitCallbacks(awareness);
			callback.testAssert(0, 0, 1, 0);
		}
	}
	
	@Test
	public void testMoveAwayBack() throws InterruptedException {
		MapCallbackRealistic callback = new MapCallbackRealistic();
		try (AwarenessHandler awareness = new AwarenessHandler(callback)) {
			initAwareness(awareness, callback);
			GenericCreatureObject creature = new GenericCreatureObject(1);
			awareness.moveObject(creature, CREATURE_LOCATION);
			awaitCallbacks(awareness);
			callback.testAssert(WITHIN_RANGE.size(), 0, 1, 0);
			callback.set(0, 0, 0, 0);
			awareness.moveObject(creature, new Location(0, 0, 0, Terrain.TATOOINE));
			awaitCallbacks(awareness);
			callback.testAssert(0, WITHIN_RANGE.size(), 1, 0);
			callback.set(0, 0, 0, 0);
			awareness.moveObject(creature, CREATURE_LOCATION);
			awaitCallbacks(awareness);
			callback.testAssert(WITHIN_RANGE.size(), 0, 1, 0);
		}
	}
	
	@Test
	public void testMoveIntoStarport() throws InterruptedException {
		MapCallbackRealistic callback = new MapCallbackRealistic();
		try (AwarenessHandler awareness = new AwarenessHandler(callback)) {
			GenericCreatureObject creature = new GenericCreatureObject(1);
			BuildingObject starport = null;
			for (SWGObject obj : WITHIN_RANGE) {
				if (obj instanceof BuildingObject && obj.getTemplate().contains("starport"))
					starport = (BuildingObject) obj;
			}
			Assert.assertNotNull("Starport is null!", starport);
			initAwareness(awareness, callback);
			awareness.moveObject(creature, CREATURE_LOCATION);
			awaitCallbacks(awareness);
			callback.testAssert(WITHIN_RANGE.size(), 0, 1, 0);
			callback.set(0, 0, 0, 0);
			awareness.moveObject(creature, CREATURE_LOCATION);
			awaitCallbacks(awareness);
			callback.testAssert(0, 0, 1, 0);
			callback.set(0, 0, 0, 0);
			awareness.moveObject(creature, starport.getCellByNumber(1), new Location(0, 0, 0, Terrain.TATOOINE));
			awaitCallbacks(awareness);
			callback.testAssert(0, 0, 0, 0);
		}
	}
	
	private void initAwareness(AwarenessHandler awareness, MapCallback callback) {
		callback.set(0, 0, 0, 0);
		for (SWGObject obj : EISLEY_OBJECTS) {
			awareness.moveObject(obj, obj.getLocation());
		}
		awaitCallbacks(awareness);
		callback.set(0, 0, 0, 0);
	}
	
	private void awaitCallbacks(AwarenessHandler awareness) {
		try {
			long timeout = 5000;
			while (!awareness.isCallbacksDone() && timeout > 0) {
				Thread.sleep(1);
				timeout--;
			}
		} catch (InterruptedException e) {
			Log.e(e);
		}
	}
	
	private static class MapCallbackRealistic extends MapCallback {
		
		@Override
		public void onWithinRange(SWGObject obj, SWGObject inRange) {
			super.onWithinRange(obj, inRange);
			obj.addObjectAware(inRange);
		}
		
		@Override
		public void onOutOfRange(SWGObject obj, SWGObject outOfRange) {
			super.onOutOfRange(obj, outOfRange);
			obj.removeObjectAware(outOfRange);
		}
		
	}
	
}
