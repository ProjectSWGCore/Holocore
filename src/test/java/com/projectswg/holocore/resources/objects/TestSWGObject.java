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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Assert.AssertionException;

import com.projectswg.holocore.test_resources.GenericCreatureObject;

@RunWith(JUnit4.class)
public class TestSWGObject {
	
	@Test
	public void testWorldLocation() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericCreatureObject(2);
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
		SWGObject child = new GenericCreatureObject(2);
		parent.setTerrain(Terrain.ADVENTURE1);
		
		Assert.assertEquals(Terrain.ADVENTURE1, parent.getTerrain());
		Assert.assertEquals(null, child.getTerrain());
		
		child.moveToContainer(parent);
		Assert.assertEquals(Terrain.ADVENTURE1, parent.getTerrain());
		Assert.assertEquals(Terrain.ADVENTURE1, child.getTerrain());
		
		parent.setTerrain(Terrain.TATOOINE);
		Assert.assertEquals(Terrain.TATOOINE, parent.getTerrain());
		Assert.assertEquals(Terrain.TATOOINE, child.getTerrain());
	}
	
	@Test(expected=AssertionException.class)
	public void testChildTerrainInvalidParent1() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericCreatureObject(2);
		parent.setTerrain(Terrain.ADVENTURE1);
		child.moveToContainer(parent);
		
		child.setTerrain(Terrain.TATOOINE);
	}
	
	@Test(expected=AssertionException.class)
	public void testChildTerrainInvalidParent2() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericCreatureObject(2);
		parent.setTerrain(Terrain.ADVENTURE1);
		child.moveToContainer(parent);
		
		child.setPosition(Terrain.CORELLIA, 0, 0, 0);
	}
	
	@Test(expected=AssertionException.class)
	public void testChildTerrainInvalidParent3() {
		SWGObject parent = new GenericCreatureObject(1);
		SWGObject child = new GenericCreatureObject(2);
		parent.setTerrain(Terrain.ADVENTURE1);
		child.moveToContainer(parent);
		
		child.setLocation(new Location(0, 0, 0, Terrain.NABOO));
	}
	
}
