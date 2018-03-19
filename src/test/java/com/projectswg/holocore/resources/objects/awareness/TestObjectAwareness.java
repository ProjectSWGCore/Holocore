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

package com.projectswg.holocore.resources.objects.awareness;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.building.BuildingObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.resources.objects.waypoint.WaypointObject;
import com.projectswg.holocore.services.objects.ObjectCreator;
import com.projectswg.holocore.test_resources.GenericCreatureObject;
import com.projectswg.holocore.test_resources.GenericTangibleObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestObjectAwareness {
	
	private ObjectAwareness awareness;
	private GenericCreatureObject player;
	private GenericCreatureObject testPlayer;
	private GenericTangibleObject testTangible;
	private BuildingObject testBuilding1;
	private BuildingObject testBuilding2;
	private WaypointObject testWaypoint;
	private CellObject testCell1;
	private CellObject testCell2;
	private TangibleObject inventoryObject;
	private TangibleObject testInventoryObject;
	
	private void initialize() {
		awareness = new ObjectAwareness();
		player = new GenericCreatureObject(1);
		testPlayer = new GenericCreatureObject(2);
		testTangible = new GenericTangibleObject(3);
		testBuilding1 = (BuildingObject) ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		testBuilding2 = (BuildingObject) ObjectCreator.createObjectFromTemplate(5, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		testCell1 = new CellObject(6);
		testCell2 = new CellObject(7);
		testWaypoint = new WaypointObject(8);
		inventoryObject = new TangibleObject(9);
		testInventoryObject = new TangibleObject(10);
		
		player.setLoadRange(100);
		testPlayer.setLoadRange(100);
		testTangible.setLoadRange(0);
		testCell1.setNumber(1);
		testCell2.setNumber(1);
		
		player.setupAsCharacter();
		testPlayer.setupAsCharacter();
		
		testCell1.moveToContainer(testBuilding1);
		testCell2.moveToContainer(testBuilding2);
		inventoryObject.moveToContainer(player.getSlottedObject("inventory"));
		testInventoryObject.moveToContainer(testPlayer.getSlottedObject("inventory"));
		
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
		player.setLoadRange(-1);
		player.setHasOwner(false);
		Assert.assertEquals(0, player.getLoadRange());
		
		moveNoAssert(TestLocation.SSI);
		System.out.println(player.getObjectsAware());
		assertAware(List.of(player));
		
		player.setHasOwner(true);
		Assert.assertNotEquals(0, player.getLoadRange());
		player.setLoadRange(100);
		Assert.assertEquals(100, player.getLoadRange());
		move(TestLocation.SSI);
		move(TestLocation.SSO);
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
		assertEquals(0, testBuilding1.getLoadRange());
		player.moveToContainer(testCell1);
		assertEquals(player.getLoadRange(), testBuilding1.getLoadRange());
	}
	
	private void moveNoAssert(TestLocation location) {
		player.moveToContainer(getParent(location.getParent()));
		player.setLocation(location.getLocation());
		awareness.updateObject(player);
	}
	
	private void move(TestLocation location) {
		player.moveToContainer(getParent(location.getParent()));
		player.setLocation(location.getLocation());
		awareness.updateObject(player);
		
		assertAware(getExpectedAware(location.getAwareSet()));
	}
	
	private void assertAware(Collection<SWGObject> awareExpected) {
		Collection<SWGObject> awareActual = player.getObjectsAware();
		
		// Ensure it doesn't contain the unexpected
		for (SWGObject a : awareActual) {
			if (a.getParent() != null)
				continue;
			assertTrue("Not supposed to be aware of object: " + a, awareExpected.contains(a));
		}
		assertFalse("Test inventory object should not be visible", awareActual.contains(testInventoryObject));
		assertTrue("Inventory object should always be visible", awareActual.contains(inventoryObject));
		
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
			case NONE:		return List.of(player);
			case TATOOINE:	return List.of(player, testPlayer, testTangible, testBuilding1, testPlayer.getSlottedObject("ghost"));
			case NABOO:		return List.of(player, testBuilding2);
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
		SSO	(TestParent.NONE,	TestAwareSet.NONE, buildTatooine(150, 150)),
		SDO	(TestParent.NONE,	TestAwareSet.NONE, buildTatooine(-150, -150)),
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
