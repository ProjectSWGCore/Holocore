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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.test_resources.GenericCreatureObject;

@RunWith(JUnit4.class)
public class TestAware {
	
	@Test
	public void testAdd() {
		GenericCreatureObject creatureA = new GenericCreatureObject(1);
		GenericCreatureObject creatureB = new GenericCreatureObject(2);
		Aware a = new Aware(creatureA);
		Aware b = new Aware(creatureB);
		a.add(b);
		Assert.assertTrue(a.contains(creatureB));
		Assert.assertTrue(b.contains(creatureA));
	}
	
	@Test
	public void testRemove() {
		GenericCreatureObject creatureA = new GenericCreatureObject(1);
		GenericCreatureObject creatureB = new GenericCreatureObject(2);
		Aware a = new Aware(creatureA);
		Aware b = new Aware(creatureB);
		a.add(b);
		Assert.assertTrue(a.contains(creatureB));
		Assert.assertTrue(b.contains(creatureA));
		a.remove(b);
		Assert.assertFalse(a.contains(creatureB));
		Assert.assertFalse(b.contains(creatureA));
	}
	
	@Test
	public void testAwareNoParent() {
		GenericCreatureObject creatureA = new GenericCreatureObject(1);
		GenericCreatureObject creatureB = new GenericCreatureObject(2);
		Aware a = new Aware(creatureA);
		Aware b = new Aware(creatureB);
		a.add(b);
		Set<SWGObject> aware = a.getAware();
		Assert.assertEquals(1, aware.size());
		Assert.assertTrue(aware.contains(creatureB));
	}
	
	@Test
	public void testObserversNoParent() {
		GenericCreatureObject creatureA = new GenericCreatureObject(1);
		GenericCreatureObject creatureB = new GenericCreatureObject(2);
		Aware a = new Aware(creatureA);
		Aware b = new Aware(creatureB);
		a.add(b);
		Assert.assertTrue(a.contains(creatureB));
		Assert.assertTrue(b.contains(creatureA));
		Set<Player> observers = a.getObservers();
		Assert.assertEquals(1, observers.size());
		Assert.assertTrue(observers.contains(creatureB.getOwner()));
	}
	
	@Test
	public void testAwareParent() {
		GenericCreatureObject creatureA = new GenericCreatureObject(1);
		GenericCreatureObject creatureB = new GenericCreatureObject(2);
		GenericCreatureObject creatureC = new GenericCreatureObject(3);
		Aware a = new Aware(creatureA);
		Aware b = new Aware(creatureB);
		Aware c = new Aware(creatureC);
		c.setParent(a);
		a.add(b);
		Set<SWGObject> aware = c.getAware();
		Assert.assertEquals(1, aware.size());
		Assert.assertTrue(aware.contains(creatureB));
	}
	
	@Test
	public void testObserversParent() {
		CellObject cellA = new CellObject(1);
		GenericCreatureObject creatureB = new GenericCreatureObject(2);
		GenericCreatureObject creatureC = new GenericCreatureObject(3);
		Aware a = new Aware(cellA);
		Aware b = new Aware(creatureB);
		Aware c = new Aware(creatureC);
		cellA.addObject(creatureC);
		c.setParent(a);
		a.add(b);
		Assert.assertTrue(a.contains(creatureB));
		Assert.assertTrue(b.contains(cellA));
		Set<Player> observers = c.getObservers();
		Assert.assertEquals(1, observers.size());
		Assert.assertTrue(observers.contains(creatureB.getOwner()));
		observers = b.getObservers();
		Assert.assertEquals(1, observers.size());
		Assert.assertTrue(observers.contains(creatureC.getOwner()));
	}
	
}
