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
package com.projectswg.holocore.services.loot;

import com.projectswg.holocore.resources.objects.creature.CreatureDifficulty;
import com.projectswg.holocore.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

public class TestRareLootService extends TestRunnerNoIntents {
	
	private final RareLootService rls;
	
	public TestRareLootService() {
		rls = new RareLootService();
	}
	
	@Test
	public void testIsPlayerEligible() {
		Assert.assertTrue(rls.isPlayerEligible(true, false));
		Assert.assertFalse(rls.isPlayerEligible(false, true));
		Assert.assertFalse(rls.isPlayerEligible(false, false));
	}
	
	@Test
	public void testIsLevelEligible() {
		Assert.assertTrue(rls.isLevelEligible(90, 84));
		Assert.assertFalse(rls.isLevelEligible(90, 83));
	}
	
	@Test
	public void testIsDrop() {
		Assert.assertTrue(rls.isDrop(1));
		Assert.assertFalse(rls.isDrop(2));
	}
	
	@Test
	public void testTemplateForDifficulty() {
		Assert.assertEquals("object/tangible/item/shared_rare_loot_chest_1.iff", rls.templateForDifficulty(CreatureDifficulty.NORMAL));
		Assert.assertEquals("object/tangible/item/shared_rare_loot_chest_2.iff", rls.templateForDifficulty(CreatureDifficulty.ELITE));
		Assert.assertEquals("object/tangible/item/shared_rare_loot_chest_3.iff", rls.templateForDifficulty(CreatureDifficulty.BOSS));
	}
	
	@Test
	public void testChestIdForTemplate() {
		Assert.assertEquals("rare_loot_chest_1", rls.chestIdForTemplate("object/tangible/item/shared_rare_loot_chest_1.iff"));
	}
	
}
