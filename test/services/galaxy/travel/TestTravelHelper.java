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
package services.galaxy.travel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

import resources.objects.SpecificObject;
import test_resources.GenericCreatureObject;

@RunWith(JUnit4.class)
public class TestTravelHelper {
	
	@Test
	public void testTravelGroups() {
		TravelHelper helper = new TravelHelper();
		Assert.assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_SHUTTLE.getTemplate()));
		Assert.assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT.getTemplate()));
		Assert.assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT_THEED.getTemplate()));
	}
	
	@Test
	public void testRoutesLoaded() {
		Terrain [] terrains = new Terrain[] {
			Terrain.CORELLIA,	Terrain.DANTOOINE,	Terrain.DATHOMIR,
			Terrain.ENDOR,		Terrain.LOK,		Terrain.NABOO,
			Terrain.RORI,		Terrain.TALUS,		Terrain.TATOOINE,
			Terrain.YAVIN4,		Terrain.MUSTAFAR,	Terrain.KASHYYYK_MAIN
		};
		TravelHelper helper = new TravelHelper();
		for (int i = 0; i < terrains.length; i++) {
			Assert.assertEquals(100, helper.getTravelFee(terrains[i], terrains[i]));
		}
		Assert.assertEquals(1250, helper.getTravelFee(Terrain.DATHOMIR, Terrain.CORELLIA));
		Assert.assertEquals(2000, helper.getTravelFee(Terrain.CORELLIA, Terrain.DATHOMIR));
		Assert.assertEquals(4000, helper.getTravelFee(Terrain.NABOO, Terrain.ENDOR));
		Assert.assertEquals(500, helper.getTravelFee(Terrain.TATOOINE, Terrain.NABOO));
	}
	
	@Test
	public void testNearestTravelPoint() {
		TravelHelper helper = new TravelHelper();
		TravelPoint starport = new TravelPoint("starport", new Location(50, 0, 50, Terrain.TATOOINE), true, true);
		TravelPoint shuttleport = new TravelPoint("shuttleport", new Location(-50, 0, -50, Terrain.TATOOINE), false, true);
		TravelPoint outOfRange = new TravelPoint("outOfRange", new Location(1000, 0, 1000, Terrain.TATOOINE), true, true);
		helper.addTravelPoint(starport);
		helper.addTravelPoint(shuttleport);
		helper.addTravelPoint(outOfRange);
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setPosition(Terrain.TATOOINE, 75, 0, 75);
		Assert.assertEquals(starport, helper.getNearestTravelPoint(creature));
		creature.setPosition(Terrain.TATOOINE, 25, 0, 25);
		Assert.assertEquals(starport, helper.getNearestTravelPoint(creature));
		creature.setPosition(Terrain.TATOOINE, 500, 0, 500);
		Assert.assertEquals(starport, helper.getNearestTravelPoint(creature));
		creature.setPosition(Terrain.TATOOINE, -25, 0, -25);
		Assert.assertEquals(shuttleport, helper.getNearestTravelPoint(creature));
		creature.setPosition(Terrain.TATOOINE, -75, 0, -75);
		Assert.assertEquals(shuttleport, helper.getNearestTravelPoint(creature));
	}
	
}
