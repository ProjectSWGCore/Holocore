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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

import resources.objects.SWGObject;
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
		Assert.assertEquals(2000, helper.getTravelFee(Terrain.DATHOMIR, Terrain.CORELLIA));
		Assert.assertEquals(2000, helper.getTravelFee(Terrain.CORELLIA, Terrain.DATHOMIR));
		Assert.assertEquals(1750, helper.getTravelFee(Terrain.NABOO, Terrain.ENDOR));
		Assert.assertEquals(500, helper.getTravelFee(Terrain.TATOOINE, Terrain.NABOO));
		Assert.assertFalse(helper.isValidRoute(Terrain.TATOOINE, Terrain.YAVIN4));
		Assert.assertTrue(helper.isValidRoute(Terrain.TATOOINE, Terrain.LOK));
		Assert.assertTrue(helper.isValidRoute(Terrain.TATOOINE, Terrain.NABOO));
	}
	
	@Test
	public void testNearestTravelPoint() {
		TravelHelper helper = new TravelHelper();
		TravelPoint starport = new TravelPoint("starport", new Location(50, 0, 50, Terrain.TATOOINE), null, true);
		TravelPoint shuttleport = new TravelPoint("shuttleport", new Location(-50, 0, -50, Terrain.TATOOINE), null, false);
		TravelPoint outOfRange = new TravelPoint("outOfRange", new Location(1000, 0, 1000, Terrain.TATOOINE), null, true);
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
	
	@Test
	public void testNoInvalidRoutesTatooine() {
		TravelHelper helper = new TravelHelper();
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setPosition(Terrain.TATOOINE, 3500, 5, -4800);
		List<TravelPoint> destinations = helper.getAvailableTravelPoints(creature, Terrain.YAVIN4);
		Assert.assertEquals(0, destinations.size());
		destinations = helper.getAvailableTravelPoints(creature, Terrain.LOK);
		Assert.assertEquals(1, destinations.size());
		Assert.assertEquals("Nym's Stronghold" , destinations.get(0).getName());
	}
	
	@Test
	public void testNoInvalidRoutesCorellia() {
		TravelHelper helper = new TravelHelper();
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setPosition(Terrain.CORELLIA, -75, 0, -4723);
		List<TravelPoint> destinations = helper.getAvailableTravelPoints(creature, Terrain.YAVIN4);
		Assert.assertEquals(3, destinations.size());
		Assert.assertEquals("Imperial Base", destinations.get(0).getName());
		Assert.assertEquals("Labor Outpost", destinations.get(1).getName());
		Assert.assertEquals("Mining Outpost", destinations.get(2).getName());
		destinations = helper.getAvailableTravelPoints(creature, Terrain.LOK);
		Assert.assertEquals(0, destinations.size());
	}
	
	@Test
	public void testValidTicketCreated() {
		TravelHelper helper = new TravelHelper();
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setupAsCharacter();
		creature.setPosition(Terrain.YAVIN4, 4054, 0, -6216);
		TravelPoint imperialBase = helper.getDestinationPoint(Terrain.YAVIN4, "Imperial Base");
		TravelPoint coronet = helper.getDestinationPoint(Terrain.CORELLIA, "Coronet Starport");
		helper.grantTicket(imperialBase, coronet, creature);
		Assert.assertEquals(imperialBase, helper.getNearestTravelPoint(creature));
		Assert.assertEquals(1, creature.getSlottedObject("inventory").getContainedObjects().size());
		System.out.println(creature.getSlottedObject("inventory").getContainedObjects());
		List<SWGObject> tickets = helper.getTickets(creature);
		Assert.assertEquals(1, tickets.size());
		
	}
	
}
