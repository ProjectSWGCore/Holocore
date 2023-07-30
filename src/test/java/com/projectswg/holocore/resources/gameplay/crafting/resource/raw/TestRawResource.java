/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.crafting.resource.raw;

import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource.RawResourceBuilder;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TestRawResource extends TestRunnerNoIntents {
	
	@Test
	public void testBuilder() {
		RawResource resource = new RawResourceBuilder(1)
				.setName("resource_name")
				.setParent(null)
				.setMinPools(15)
				.setMaxPools(30)
				.setMinTypes(45)
				.setMaxTypes(60)
				.setRecycled(false)
				.setCrateTemplate("")
				.build();
		assertEquals("resource_name", resource.getName());
		assertNull(resource.getParent());
		assertEquals(15, resource.getMinPools());
		assertEquals(30, resource.getMaxPools());
		assertEquals(45, resource.getMinTypes());
		assertEquals(60, resource.getMaxTypes());
		assertEquals("", resource.getCrateTemplate());
		assertFalse(resource.isRecycled());
		
		RawResource child = new RawResourceBuilder(2)
				.setName("child_name")
				.setParent(resource)
				.setMinPools(5)
				.setMaxPools(6)
				.setMinTypes(7)
				.setMaxTypes(8)
				.setRecycled(true)
				.setCrateTemplate("")
				.build();
		assertEquals("child_name", child.getName());
		assertEquals(resource, child.getParent());
		assertEquals(5, child.getMinPools());
		assertEquals(6, child.getMaxPools());
		assertEquals(7, child.getMinTypes());
		assertEquals(8, child.getMaxTypes());
		assertEquals("", child.getCrateTemplate());
		assertTrue(child.isRecycled());
	}
	
}
