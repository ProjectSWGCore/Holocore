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

package com.projectswg.holocore.services.faction;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.holocore.resources.objects.creature.CreatureDifficulty;
import com.projectswg.holocore.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class TestCivilWarService extends TestRunnerNoIntents {
	
	private final CivilWarService service;
	
	public TestCivilWarService() {
		service = new CivilWarService();
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
	
	private int epochTime(LocalDate date) {
		return (int) date.toEpochSecond(LocalTime.MIDNIGHT, OffsetDateTime.now().getOffset());
	}
	
	@Test
	public void testNextUpdateTime() {
		LocalDate now = LocalDate.of(2018, 2, 19);	// It's a Monday
		LocalDate rankDay = LocalDate.of(2018, 2, 23);	// It's a Friday, the exact time we rank up
		LocalDate dayAfter = LocalDate.of(2018, 2, 24);	// It's a Saturday, 24 hours after rank up
		LocalDate nextRankDay = LocalDate.of(2018, 3, 2);	// It's a Friday, exactly one week after first rank up
		
		int nowRankTime = epochTime(rankDay);
		int nextRankTime = epochTime(nextRankDay);
		
		Assert.assertEquals(nowRankTime, service.nextUpdateTime(now));
		Assert.assertEquals(nextRankTime, service.nextUpdateTime(rankDay));	// When we hit the scheduled rank time, the next update should be in a week
		Assert.assertEquals(nextRankTime, service.nextUpdateTime(dayAfter));	// Next time should be in six days (from Saturday to Friday)
	}
	
	@Test
	public void testIsDecayRank() {
		Assert.assertFalse(service.isDecayRank(6));
		Assert.assertTrue(service.isDecayRank(7));
	}
	
	@Test
	public void testRankProgress() {
		Assert.assertEquals(2.86f, service.rankProgress(10.0f, 20.0f, 7, 9000), 1);
	}
	
	@Test
	public void testIsRankDown() {
		Assert.assertFalse(service.isRankDown(40, 50));
		Assert.assertTrue(service.isRankDown(3, -10));
	}
	
	@Test
	public void testLeftoverPoints() {
		Assert.assertEquals(6000, service.leftoverPoints(130, 20000));
	}
	
}
