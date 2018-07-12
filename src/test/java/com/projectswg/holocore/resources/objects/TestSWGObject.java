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
package com.projectswg.holocore.resources.objects;

import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.runners.TestRunnerNoIntents;
import com.projectswg.holocore.test_resources.GenericTangibleObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

import com.projectswg.holocore.test_resources.GenericCreatureObject;

import java.util.Collection;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class TestSWGObject extends TestRunnerNoIntents {
	
	@Test
	public void testLineOfSight() {
		SWGObject a = new GenericTangibleObject(1);
		SWGObject b = new GenericTangibleObject(2);
		BuildingObject buio = (BuildingObject) ObjectCreator.createObjectFromTemplate(3, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		buio.setPosition(Terrain.TATOOINE, 10, 0, 10);
		buio.setHeading(45);
		buio.populateCells();
		a.setTerrain(Terrain.TATOOINE);
		b.setTerrain(Terrain.TATOOINE);
		
		/*
		 * Portal[CellObject[7 '' cell/shared_cell.iff] -> CellObject[6 '' cell/shared_cell.iff]  [Point3D[-7.23, 0.64, 0.22], Point3D[-4.71, 0.64, 0.22]] height=3.91]
		 * Portal[CellObject[6 '' cell/shared_cell.iff] -> CellObject[5 '' cell/shared_cell.iff]  [Point3D[-4.42, 0.70, 4.30], Point3D[-4.42, 0.70, 6.30]] height=3.35]
		 * Portal[CellObject[5 '' cell/shared_cell.iff] -> null  [Point3D[0.05, 0.63, 6.76], Point3D[2.04, 0.63, 6.76]] height=3.41]]
		 */
		
		// Testing portal 6-7
		a.systemMove(buio.getCellByNumber(2));
		b.systemMove(buio.getCellByNumber(3));
		a.setPosition(-5.97, 0.64, 1);	// Center of portal
		b.setPosition(-5.97, 0.64, -1);
		Assert.assertTrue(a.isLineOfSight(b));
		Assert.assertTrue(b.isLineOfSight(a));
		a.setPosition(-7.20, 0.64, 1);	// Edge of portal
		b.setPosition(-7.20, 0.64, -1);
		Assert.assertTrue(a.isLineOfSight(b));
		Assert.assertTrue(b.isLineOfSight(a));
		a.setPosition(-10, 0.64, 1);	// Far beyond portal's view
		b.setPosition(-7.20, 0.64, -1);
		Assert.assertFalse(a.isLineOfSight(b));
		Assert.assertFalse(b.isLineOfSight(a));
		
		// Testing portal null-5
		a.systemMove(null);
		b.systemMove(buio.getCellByNumber(1));
		// Center of portal
		a.setLocation(Location.builder().setTerrain(Terrain.TATOOINE).setPosition(1, 0.63, 7.5).translateLocation(buio.getLocation()).build());
		b.setPosition(1, 0.63, 5.5);
		Assert.assertTrue(a.isLineOfSight(b));
		Assert.assertTrue(b.isLineOfSight(a));
		// Edge of portal
		a.setLocation(Location.builder().setTerrain(Terrain.TATOOINE).setPosition(2, 0.63, 7.5).translateLocation(buio.getLocation()).build());
		b.setPosition(2, 0.63, 5.5);
		Assert.assertTrue(a.isLineOfSight(b));
		Assert.assertTrue(b.isLineOfSight(a));
		// Just beyond portal's view
		a.setLocation(Location.builder().setTerrain(Terrain.TATOOINE).setPosition(3, 0.63, 7.5).translateLocation(buio.getLocation()).build());
		b.setPosition(2, 0.63, 5.5);
		Assert.assertFalse(a.isLineOfSight(b));
		Assert.assertFalse(b.isLineOfSight(a));
	}
	
	@Test
	public void testWorldLocation() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericTangibleObject(2);
		child.moveToContainer(parent);
		
		Location worldLocation = new Location(15, 17, 19, Terrain.NABOO);
		parent.setLocation(worldLocation);
		child.setPosition(0, 0, 0);
		
		Assert.assertEquals(worldLocation, parent.getWorldLocation());
		Assert.assertEquals(worldLocation, parent.getLocation());
		Assert.assertEquals(worldLocation, child.getWorldLocation());
		Assert.assertEquals(new Location(0, 0, 0, Terrain.NABOO), child.getLocation());
		
		child.setPosition(5, 5, 5);
		Assert.assertEquals(new Location(20, 22, 24, Terrain.NABOO), child.getWorldLocation());
		Assert.assertEquals(new Location(5, 5, 5, Terrain.NABOO), child.getLocation());
	}
	
	@Test
	public void testChildTerrainUpdates() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericTangibleObject(2);
		parent.setTerrain(Terrain.ADVENTURE1);
		
		Assert.assertEquals(Terrain.ADVENTURE1, parent.getTerrain());
		Assert.assertEquals(Terrain.GONE, child.getTerrain());
		
		child.moveToContainer(parent);
		Assert.assertEquals(Terrain.ADVENTURE1, parent.getTerrain());
		Assert.assertEquals(Terrain.ADVENTURE1, child.getTerrain());
		
		parent.setTerrain(Terrain.TATOOINE);
		Assert.assertEquals(Terrain.TATOOINE, parent.getTerrain());
		Assert.assertEquals(Terrain.TATOOINE, child.getTerrain());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testChildTerrainInvalidParent1() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericTangibleObject(2);
		parent.setTerrain(Terrain.ADVENTURE1);
		child.moveToContainer(parent);
		
		child.setTerrain(Terrain.TATOOINE);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testChildTerrainInvalidParent2() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericTangibleObject(2);
		parent.setTerrain(Terrain.ADVENTURE1);
		child.moveToContainer(parent);
		
		child.setPosition(Terrain.CORELLIA, 0, 0, 0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testChildTerrainInvalidParent3() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericTangibleObject(2);
		parent.setTerrain(Terrain.ADVENTURE1);
		child.moveToContainer(parent);
		
		child.setLocation(new Location(0, 0, 0, Terrain.NABOO));
	}
	
}
