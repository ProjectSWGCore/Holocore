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
package com.projectswg.holocore.resources.gameplay.crafting.resource.raw;

import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource.RawResourceBuilder;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

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
		Assert.assertEquals("resource/resource_names", resource.getName().getFile());
		Assert.assertEquals("resource_name", resource.getName().getKey());
		Assert.assertNull(resource.getParent());
		Assert.assertEquals(15, resource.getMinPools());
		Assert.assertEquals(30, resource.getMaxPools());
		Assert.assertEquals(45, resource.getMinTypes());
		Assert.assertEquals(60, resource.getMaxTypes());
		Assert.assertEquals("", resource.getCrateTemplate());
		Assert.assertFalse(resource.isRecycled());
		
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
		Assert.assertEquals("resource/resource_names", child.getName().getFile());
		Assert.assertEquals("child_name", child.getName().getKey());
		Assert.assertEquals(resource, child.getParent());
		Assert.assertEquals(5, child.getMinPools());
		Assert.assertEquals(6, child.getMaxPools());
		Assert.assertEquals(7, child.getMinTypes());
		Assert.assertEquals(8, child.getMaxTypes());
		Assert.assertEquals("", child.getCrateTemplate());
		Assert.assertTrue(child.isRecycled());
	}
	
}
