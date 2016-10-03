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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import resources.Location;
import resources.Terrain;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.creature.CreatureObject;
import services.objects.ClientBuildoutService;

@RunWith(JUnit4.class)
public class TestAwarenessHandler {
	
	private static final Location CREATURE_LOCATION = new Location(3618, 5, -4801, Terrain.TATOOINE);
	
	private List<SWGObject> eisleyObjects = new ArrayList<>();
	
	@Before
	public void initTatooine() {
		ClientBuildoutService buildoutService = new ClientBuildoutService();
		Collection<SWGObject> allObjects = buildoutService.loadClientObjectsByArea(843); // mos eisley's area id
		eisleyObjects = allObjects.stream().filter((obj) -> {
			return obj.getTerrain() == Terrain.TATOOINE && obj.getParent() == null;
		}).collect(Collectors.toList());
		allObjects = buildoutService.loadClientObjectsByArea(-59); // general tatooine's area id
		eisleyObjects.addAll(allObjects.stream().filter((obj) -> {
			return obj.getTerrain() == Terrain.TATOOINE && obj.getParent() == null && CREATURE_LOCATION.isWithinFlatDistance(obj.getLocation(), 1024*Math.sqrt(2));
		}).collect(Collectors.toList()));
	}
	
	@Test
	public void testMoveSameAwareness() throws InterruptedException {
		MapCallbackRealistic callback = new MapCallbackRealistic();
		AwarenessHandler awareness = new AwarenessHandler(callback);
		GenericCreatureObject creature = new GenericCreatureObject(1);
		List<SWGObject> withinRange = new ArrayList<>();
		for (SWGObject obj : eisleyObjects) {
			awareness.moveObject(obj, obj.getLocation());
			if (isValidWithinRange(creature, obj, CREATURE_LOCATION, Math.max(obj.getLoadRange(), creature.getLoadRange())))
				withinRange.add(obj);
		}
		callback.waitFor(0, 0, eisleyObjects.size(), 0, 1000);
		callback.set(0, 0, 0, 0);
		awareness.moveObject(creature, CREATURE_LOCATION);
		callback.waitAndTest(withinRange.size(), 0, 1, 0, 1000);
		callback.set(0, 0, 0, 0);
		awareness.moveObject(creature, CREATURE_LOCATION);
		callback.waitAndTest(0, 0, 1, 0, 1000);
	}
	
	@Test
	public void testMoveAway() throws InterruptedException {
		MapCallbackRealistic callback = new MapCallbackRealistic();
		AwarenessHandler awareness = new AwarenessHandler(callback);
		GenericCreatureObject creature = new GenericCreatureObject(1);
		List<SWGObject> withinRange = new ArrayList<>();
		for (SWGObject obj : eisleyObjects) {
			awareness.moveObject(obj, obj.getLocation());
			if (isValidWithinRange(creature, obj, CREATURE_LOCATION, Math.max(obj.getLoadRange(), creature.getLoadRange())))
				withinRange.add(obj);
		}
		callback.waitFor(0, 0, eisleyObjects.size(), 0, 1000);
		callback.set(0, 0, 0, 0);
		awareness.moveObject(creature, CREATURE_LOCATION);
		callback.waitAndTest(withinRange.size(), 0, 1, 0, 1000);
		callback.set(0, 0, 0, 0);
		awareness.moveObject(creature, new Location(0, 0, 0, Terrain.TATOOINE));
		callback.waitAndTest(0, withinRange.size(), 1, 0, 1000);
	}
	
	@Test
	public void testMoveIntoStarport() throws InterruptedException {
		MapCallbackRealistic callback = new MapCallbackRealistic();
		AwarenessHandler awareness = new AwarenessHandler(callback);
		GenericCreatureObject creature = new GenericCreatureObject(1);
		List<SWGObject> withinRange = new ArrayList<>();
		BuildingObject starport = null;
		for (SWGObject obj : eisleyObjects) {
			awareness.moveObject(obj, obj.getLocation());
			if (isValidWithinRange(creature, obj, CREATURE_LOCATION, Math.max(obj.getLoadRange(), creature.getLoadRange()))) {
				withinRange.add(obj);
				if (obj instanceof BuildingObject && obj.getTemplate().contains("starport"))
					starport = (BuildingObject) obj;
			}
		}
		Assert.assertNotNull("Starport is null!", starport);
		callback.waitFor(0, 0, eisleyObjects.size(), 0, 1000);
		callback.set(0, 0, 0, 0);
		awareness.moveObject(creature, CREATURE_LOCATION);
		callback.waitAndTest(withinRange.size(), 0, 1, 0, 1000);
		callback.set(0, 0, 0, 0);
		awareness.moveObject(creature, starport.getCellByNumber(1), new Location(0, 0, 0, Terrain.TATOOINE));
		callback.waitAndTest(0, 0, 0, 0, 1000);
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
