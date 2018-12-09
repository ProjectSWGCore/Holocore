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

package com.projectswg.holocore.resources.support.objects.awareness;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericTangibleObject;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestObjectAwareness extends TestRunnerNoIntents {
	
	private ObjectAwareness awareness = null;
	private GenericCreatureObject player = null;
	private GenericCreatureObject testPlayer = null;
	private GenericTangibleObject testTangible = null;
	private BuildingObject testBuilding1 = null;
	private BuildingObject testBuilding2 = null;
	private WaypointObject testWaypoint = null;
	private CellObject testCell1 = null;
	private CellObject testCell2 = null;
	private SWGObject inventoryObject = null;
	
	private void initialize() {
		awareness = new ObjectAwareness();
		player = new GenericCreatureObject(getUniqueId(), "player");
		testPlayer = new GenericCreatureObject(getUniqueId(), "testPlayer");
		testTangible = new GenericTangibleObject(getUniqueId(), "testTangible");
		testBuilding1 = (BuildingObject) ObjectCreator.createObjectFromTemplate(getUniqueId(), "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		testBuilding2 = (BuildingObject) ObjectCreator.createObjectFromTemplate(getUniqueId(), "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		testCell1 = new CellObject(getUniqueId());
		testCell2 = new CellObject(getUniqueId());
		testWaypoint = new WaypointObject(getUniqueId());
		inventoryObject = player.getInventory();
		
		testBuilding1.setObjectName("building1");
		testBuilding2.setObjectName("building2");
		testCell1.setNumber(1);
		testCell2.setNumber(1);
		
		testCell1.systemMove(testBuilding1);
		testCell2.systemMove(testBuilding2);
		
		testPlayer.setLocation(buildTatooine(40, 40));
		testTangible.setLocation(buildTatooine(50, 50));
		testBuilding1.setLocation(buildTatooine(45, 45));
		testBuilding2.setLocation(buildNaboo(45, 45));
		testWaypoint.setLocation(buildTatooine(41, 41));
		
		awareness.createObject(testPlayer);
		awareness.createObject(testTangible);
		awareness.createObject(testBuilding1);
		awareness.createObject(testBuilding2);
		awareness.createObject(testCell1);
		awareness.createObject(testWaypoint);
	}
	
	@Test
	public void testTour() {
		testCombination(new ArrayList<>(), 2);
	}
	
	private void testCombination(List<TestLocation> tests, int depth) {
		if (depth == 0) {
			try {
				initialize();
				for (TestLocation test : tests)
					move(test);
			} catch (AssertionError e) {
				throw new AssertionError("Failed " + tests, e);
			}
			return;
		}
		for (TestLocation test : TestLocation.values()) {
			List<TestLocation> recurse = new ArrayList<>(tests);
			recurse.add(test);
			testCombination(recurse, depth-1);
		}
	}
	
	@Test
	public void testPlayerZoneIn() {
		initialize();
		player.setHasOwner(false);
		
		Assert.assertFalse(player.isLoggedInPlayer());
		Assert.assertTrue(testPlayer.isLoggedInPlayer());
		
		// Shouldn't be aware of anything else because it's a logged out player
		for (TestLocation loc : TestLocation.values()) {
			moveNoAssert(loc);
			assertAware(List.of(player));
		}
		
		player.setHasOwner(true);
		Assert.assertTrue(player.isLoggedInPlayer());
		Assert.assertTrue(testPlayer.isLoggedInPlayer());
		
		for (TestLocation loc : TestLocation.values()) {
			move(loc);
		}
	}
	
	@Test
	public void testDestroyWaypoint() {
		initialize();
		move(TestLocation.SSI);
		awareness.destroyObject(testWaypoint);
		assertAware(getExpectedAware(TestLocation.SSI.getAwareSet()));
	}
	
	@Test
	public void testDestroyBuilding() {
		initialize();
		move(TestLocation.SSI);
		awareness.destroyObject(testBuilding1);
		assertAware(List.of(player, testPlayer, testTangible));
	}
	
	@Test
	public void testLoadRangeUpdate() {
		initialize();
		move(TestLocation.SSI);
		player.moveToContainer(testCell1);
	}
	
	private void moveNoAssert(TestLocation location) {
		player.systemMove(getParent(location.getParent()), location.getLocation());
		
		awareness.updateObject(player);
	}
	
	private void move(TestLocation location) {
		player.systemMove(getParent(location.getParent()), location.getLocation());
		awareness.updateObject(player);
		
		assertAware(getExpectedAware(location.getAwareSet()));
	}
	
	private void assertAware(Collection<SWGObject> awareExpected) {
		Collection<SWGObject> awareActual = player.getAware();
		
		// Ensure it doesn't contain the unexpected
		for (SWGObject a : awareActual) {
			if (a.getParent() != null)
				continue;
			assertTrue("Not supposed to be aware of object: " + a, awareExpected.contains(a));
		}
		
		// Ensure it contains the expected
		for (SWGObject a : awareExpected) {
			assertTrue("Supposed to be aware of object: " + a, awareActual.contains(a));
		}
	}
	
	private SWGObject getParent(TestParent parent) {
		switch (parent) {
			case NONE:	return null;
			case BUIO1:	return testCell1;
			case BUIO2:	return testCell2;
		}
		throw new RuntimeException("Invalid test parent: " + parent);
	}
	
	private Collection<SWGObject> getExpectedAware(TestAwareSet awareSet) {
		switch (awareSet) {
			case NONE:		return List.of(player, inventoryObject);
			case TATOOINE:	return List.of(player, inventoryObject, testPlayer, testTangible, testBuilding1, testPlayer.getSlottedObject("ghost"));
			case NABOO:		return List.of(player, inventoryObject, testBuilding2);
		}
		throw new RuntimeException("Invalid test aware set: " + awareSet);
	}
	
	private static Location buildTatooine(double x, double z) {
		return Location.builder().setTerrain(Terrain.TATOOINE).setPosition(x, 0, z).build();
	}
	
	private static Location buildNaboo(double x, double z) {
		return Location.builder().setTerrain(Terrain.NABOO).setPosition(x, 0, z).build();
	}
	
	private enum TestParent {
		NONE,
		BUIO1,
		BUIO2
	}
	
	private enum TestAwareSet {
		NONE,
		TATOOINE,
		NABOO
	}
	
	private enum TestLocation {
		SSI	(TestParent.NONE,	TestAwareSet.TATOOINE, buildTatooine(25, 25)),
		SDI	(TestParent.NONE,	TestAwareSet.TATOOINE, buildTatooine(-10, -10)),
		SSO	(TestParent.NONE,	TestAwareSet.NONE, buildTatooine(3000, 3000)),
		SDO	(TestParent.NONE,	TestAwareSet.NONE, buildTatooine(-3000, -3000)),
		BSSI(TestParent.BUIO1,	TestAwareSet.TATOOINE, buildTatooine(0, 0)),
		DDO	(TestParent.NONE,	TestAwareSet.NABOO, buildNaboo(25, 25)),
		BDDO(TestParent.BUIO2,	TestAwareSet.NABOO, buildNaboo(0, 0)),
		TAT_OOM_CT	(TestParent.NONE, TestAwareSet.NONE, buildTatooine(25, 17000)),
		TAT_OOM_CB	(TestParent.NONE, TestAwareSet.NONE, buildTatooine(25, -17000)),
		TAT_OOM_RC	(TestParent.NONE, TestAwareSet.NONE, buildTatooine(17000, 25)),
		TAT_OOM_LC	(TestParent.NONE, TestAwareSet.NONE, buildTatooine(-17000, 25)),
		TAT_OOM_RT	(TestParent.NONE, TestAwareSet.NONE, buildTatooine(17000, 17000)),
		TAT_OOM_RB	(TestParent.NONE, TestAwareSet.NONE, buildTatooine(17000, -17000)),
		TAT_OOM_LT	(TestParent.NONE, TestAwareSet.NONE, buildTatooine(-17000, 17000)),
		TAT_OOM_LB	(TestParent.NONE, TestAwareSet.NONE, buildTatooine(-17000, -17000));
		
		private final TestParent parent;
		private final TestAwareSet awareSet;
		private final Location location;
		
		TestLocation(TestParent parent, TestAwareSet awareSet, Location location) {
			this.parent = parent;
			this.awareSet = awareSet;
			this.location = location;
		}
		
		public TestParent getParent() {
			return parent;
		}
		
		public TestAwareSet getAwareSet() {
			return awareSet;
		}
		
		public Location getLocation() {
			return location;
		}
		
	}
	
}
