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

package com.projectswg.holocore.resources.support.objects.swg.creature;

import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.test_resources.GenericCreatureObject;
import com.projectswg.holocore.test_resources.GenericTangibleObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestCreatureObjectAwareness {
	
	private CreatureObjectAwareness awareness;
	private CreatureObject creature;
	private TangibleObject testObject1;
	private TangibleObject testObject2;
	private Player dummy;
	
	@Before
	public void initialize() {
		creature = new GenericCreatureObject(1);
		creature.setOwner(new Player());
		awareness = new CreatureObjectAwareness(creature);
		testObject1 = new GenericTangibleObject(2);
		testObject2 = new GenericTangibleObject(3);
	}
	
	@Test
	public void testSingleObjectAdd() {
		awareness.addAware(testObject1);
		assertCreate(testObject1);
	}
	
	@Test
	public void testSingleObjectRemove() {
		awareness.addAware(testObject1);
		awareness.removeAware(testObject1);
		assertCreate();
	}
	
	@Test
	public void testSingleObjectAddFlushRemove() {
		awareness.addAware(testObject1);
		awareness.flushAware();
		assertCreate();
		assertDestroy();
		awareness.removeAware(testObject1);
		assertDestroy(testObject1);
	}
	
	@Test
	public void testSingleObjectAddNoParent() {
		testObject1.moveToContainer(testObject2);
		awareness.addAware(testObject1);
		assertCreate();
	}
	
	@Test
	public void testParentObjectRemoveNoParent() {
		testObject1.moveToContainer(testObject2);
		awareness.addAware(testObject1);
		awareness.addAware(testObject2);
		awareness.flushAware();
		assertCreate();
		assertDestroy();
		awareness.removeAware(testObject1);
		assertDestroy(testObject1);
	}
	
	@Test
	public void testParentObjectAdd() {
		testObject1.moveToContainer(testObject2);
		awareness.addAware(testObject2);
		awareness.addAware(testObject1);
		assertCreate(testObject2, testObject1);
	}
	
	@Test
	public void testParentObjectRemove() {
		testObject1.moveToContainer(testObject2);
		awareness.addAware(testObject1);
		awareness.addAware(testObject2);
		awareness.flushAware();
		assertCreate();
		assertDestroy();
		awareness.removeAware(testObject1);
		awareness.removeAware(testObject2);
		assertDestroy(testObject1, testObject2);
	}
	
	@Test
	public void testParentObjectRemoveAlreadyGone() {
		testObject1.moveToContainer(testObject2);
		awareness.addAware(testObject1);
		awareness.addAware(testObject2);
		awareness.flushAware();
		assertCreate();
		assertDestroy();
		awareness.removeAware(testObject2);
		awareness.flushAware();
		awareness.removeAware(testObject1);
		assertDestroy();
	}
	
	@Test
	public void testParentObjectAddRemove() {
		testObject1.moveToContainer(testObject2);
		awareness.addAware(testObject2);
		awareness.addAware(testObject1);
		awareness.removeAware(testObject2);
		assertCreate();
	}
	
	private void assertCreate(SWGObject ... objects) {
		Assert.assertArrayEquals(objects, awareness.getCreateList().toArray());
	}
	
	private void assertDestroy(SWGObject ... objects) {
		Assert.assertArrayEquals(objects, awareness.getDestroyList().toArray());
	}
	
}
