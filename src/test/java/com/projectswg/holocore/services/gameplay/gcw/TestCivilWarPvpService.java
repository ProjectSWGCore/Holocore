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

package com.projectswg.holocore.services.gameplay.gcw;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

public class TestCivilWarPvpService extends TestRunnerNoIntents {
	
	private final CivilWarPvpService service;
	
	public TestCivilWarPvpService() {
		service = new CivilWarPvpService();
	}
	
	@Test
	public void testIsFactionEligible() {
		Assert.assertTrue(service.isFactionEligible(PvpFaction.REBEL, PvpFaction.IMPERIAL));
		Assert.assertFalse(service.isFactionEligible(PvpFaction.NEUTRAL, PvpFaction.NEUTRAL));
		Assert.assertFalse(service.isFactionEligible(PvpFaction.NEUTRAL, PvpFaction.REBEL));
		Assert.assertFalse(service.isFactionEligible(PvpFaction.NEUTRAL, PvpFaction.IMPERIAL));
		Assert.assertFalse(service.isFactionEligible(PvpFaction.REBEL, PvpFaction.REBEL));
	}
	
	@Test
	public void testMakeMultiplier() {
		Assert.assertEquals(1, service.makeMultiplier(false, false));
		Assert.assertEquals(2, service.makeMultiplier(true, false));
		Assert.assertEquals(19, service.makeMultiplier(false, true));
		Assert.assertEquals(20, service.makeMultiplier(true, true));
	}
	
	@Test
	public void testBaseForDifficulty() {
		Assert.assertEquals(5, service.baseForDifficulty(CreatureDifficulty.NORMAL));
		Assert.assertEquals(10, service.baseForDifficulty(CreatureDifficulty.ELITE));
		Assert.assertEquals(15, service.baseForDifficulty(CreatureDifficulty.BOSS));
	}
	
	@Test
	public void testPointsGranted() {
		Assert.assertEquals(200, service.pointsGranted(10, (byte) 20));
	}
	
}
