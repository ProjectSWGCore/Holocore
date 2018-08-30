/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/

package com.projectswg.holocore.resources.support.objects.permissions;

import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericTangibleObject;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestContainerPermissions extends TestRunnerNoIntents {
	
	@Test
	public void testDefaultPermissions() {
		GenericCreatureObject a = new GenericCreatureObject(1);
		GenericCreatureObject b = new GenericCreatureObject(2);
		TangibleObject inventoryA = new GenericTangibleObject(3);
		TangibleObject inventoryB = new GenericTangibleObject(4);
		inventoryA.systemMove(a.getInventory());
		inventoryB.systemMove(b.getInventory());
		
		assertTrue(a.isVisible(b));
		assertTrue(b.isVisible(a));
		
		assertTrue(a.getSlottedObject("ghost").isVisible(a));
		assertTrue(b.getSlottedObject("ghost").isVisible(b));
		assertTrue(a.getSlottedObject("ghost").isVisible(b));
		assertTrue(b.getSlottedObject("ghost").isVisible(a));
		
		assertTrue(a.getInventory().isVisible(a));
		assertTrue(b.getInventory().isVisible(b));
		assertTrue(inventoryA.isVisible(a));
		assertTrue(inventoryB.isVisible(b));
		assertFalse(inventoryA.isVisible(b));
		assertFalse(inventoryB.isVisible(a));
	}
	
	@Test
	public void testAdminPermissions() {
		GenericCreatureObject a = new GenericCreatureObject(1);
		GenericCreatureObject b = new GenericCreatureObject(2);
		TangibleObject inventoryA = new GenericTangibleObject(3);
		TangibleObject inventoryB = new GenericTangibleObject(4);
		inventoryA.systemMove(a.getInventory());
		inventoryB.systemMove(b.getInventory());
		
		b.setContainerPermissions(AdminPermissions.getPermissions());
		b.addAbility("admin");
		
		assertTrue(a.isVisible(b));
		assertFalse(b.isVisible(a));
		
		assertTrue(a.getSlottedObject("ghost").isVisible(a));
		assertTrue(b.getSlottedObject("ghost").isVisible(b));
		assertTrue(a.getSlottedObject("ghost").isVisible(b));
		assertFalse(b.getSlottedObject("ghost").isVisible(a));
		
		assertTrue(a.getInventory().isVisible(a));
		assertTrue(b.getInventory().isVisible(b));
	}
	
}
