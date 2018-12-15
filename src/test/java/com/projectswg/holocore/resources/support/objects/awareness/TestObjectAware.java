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

import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericTangibleObject;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TestObjectAware extends TestRunnerNoIntents {
	
	private SWGObject tangible1;
	private SWGObject tangible2;
	private SWGObject creature1;
	private SWGObject creature2;
	
	@Before
	public void initialize() {
		tangible1 = new GenericTangibleObject(1);
		tangible2 = new GenericTangibleObject(2);
		creature1 = new GenericCreatureObject(3);
		creature2 = new GenericCreatureObject(4);
		
		assertFalse(tangible1.getAware().contains(tangible2));
		assertFalse(tangible2.getAware().contains(tangible1));
		assertFalse(creature1.getObservers().contains(creature2.getOwner()));
		assertFalse(creature2.getObservers().contains(creature1.getOwner()));
	}
	
	@Test
	public void testSingleBucketAware() {
		tangible1.setAware(AwarenessType.OBJECT, List.of(tangible2));
		assertTrue(tangible1.getAware().contains(tangible2));
		assertFalse(tangible2.getAware().contains(tangible1));
		assertTrue(tangible1.getAware(AwarenessType.OBJECT).contains(tangible2));
		assertFalse(tangible2.getAware(AwarenessType.OBJECT).contains(tangible1));
		assertFalse(tangible1.getAware(AwarenessType.GROUP).contains(tangible2));
		assertFalse(tangible2.getAware(AwarenessType.GROUP).contains(tangible1));
		
		tangible1.setAware(AwarenessType.OBJECT, List.of());
		assertFalse(tangible1.getAware().contains(tangible2));
		assertFalse(tangible2.getAware().contains(tangible1));
		assertFalse(tangible1.getAware(AwarenessType.OBJECT).contains(tangible2));
		assertFalse(tangible2.getAware(AwarenessType.OBJECT).contains(tangible1));
		assertFalse(tangible1.getAware(AwarenessType.GROUP).contains(tangible2));
		assertFalse(tangible2.getAware(AwarenessType.GROUP).contains(tangible1));
		
		// Add both directions
		tangible1.setAware(AwarenessType.OBJECT, List.of(tangible2));
		assertTrue(tangible1.getAware().contains(tangible2));
		assertFalse(tangible2.getAware().contains(tangible1));
		
		tangible2.setAware(AwarenessType.OBJECT, List.of(tangible1));
		assertTrue(tangible1.getAware().contains(tangible2));
		assertTrue(tangible2.getAware().contains(tangible1));
		
		tangible1.setAware(AwarenessType.OBJECT, List.of());
		assertFalse(tangible1.getAware().contains(tangible2));
		assertTrue(tangible2.getAware().contains(tangible1));
	}
	
	@Test
	public void testDoubleBucketAware() {
		tangible1.setAware(AwarenessType.OBJECT, List.of(tangible2));
		assertTrue(tangible1.getAware().contains(tangible2));
		assertFalse(tangible2.getAware().contains(tangible1));
		
		tangible1.setAware(AwarenessType.GROUP, List.of(tangible2));
		assertTrue(tangible1.getAware().contains(tangible2));
		assertFalse(tangible2.getAware().contains(tangible1));
		
		tangible1.setAware(AwarenessType.OBJECT, List.of());
		assertTrue(tangible1.getAware().contains(tangible2));
		assertFalse(tangible2.getAware().contains(tangible1));
		assertFalse(tangible1.getAware(AwarenessType.OBJECT).contains(tangible2));
		assertFalse(tangible2.getAware(AwarenessType.OBJECT).contains(tangible1));
		assertTrue(tangible1.getAware(AwarenessType.GROUP).contains(tangible2));
		assertFalse(tangible2.getAware(AwarenessType.GROUP).contains(tangible1));
		
		tangible1.setAware(AwarenessType.GROUP, List.of());
		assertFalse(tangible1.getAware().contains(tangible2));
		assertFalse(tangible2.getAware().contains(tangible1));
		assertFalse(tangible1.getAware(AwarenessType.OBJECT).contains(tangible2));
		assertFalse(tangible2.getAware(AwarenessType.OBJECT).contains(tangible1));
		assertFalse(tangible2.getAware(AwarenessType.GROUP).contains(tangible1));
		assertFalse(tangible1.getAware(AwarenessType.GROUP).contains(tangible2));
	}
	
}
