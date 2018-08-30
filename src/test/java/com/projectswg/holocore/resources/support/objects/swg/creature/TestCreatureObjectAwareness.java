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

package com.projectswg.holocore.resources.support.objects.swg.creature;

import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericTangibleObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestCreatureObjectAwareness extends TestRunnerNoIntents {
	
	private CreatureObjectAwareness awareness;
	private CreatureObject creature;
	private TangibleObject testObject1;
	private TangibleObject testObject2;
	
	@Before
	public void initialize() {
		creature = new GenericCreatureObject(getUniqueId());
		awareness = new CreatureObjectAwareness(creature);
		testObject1 = new GenericTangibleObject(getUniqueId(), "obj1");
		testObject2 = new GenericTangibleObject(getUniqueId(), "obj2");
		syncSelfAware();
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
	public void testParentObjectAddRemove() {
		testObject1.moveToContainer(testObject2);
		awareness.addAware(testObject2);
		awareness.addAware(testObject1);
		awareness.removeAware(testObject2);
		assertCreate();
	}
	
	private void assertCreate(SWGObject ... objects) {
		Assert.assertEquals(List.of(objects), awareness.getCreateList());
	}
	
	private void assertDestroy(SWGObject ... objects) {
		Assert.assertEquals(List.of(objects), awareness.getDestroyList());
	}
	
	private void syncSelfAware() {
		for (SWGObject self : creature.getAware(AwarenessType.SELF))
			awareness.addAware(self);
		awareness.flushAware();
	}
	
}
