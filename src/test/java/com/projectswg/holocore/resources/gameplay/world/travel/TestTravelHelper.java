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
package com.projectswg.holocore.resources.gameplay.world.travel;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.objects.SpecificObject;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestTravelHelper extends TestRunnerNoIntents {
	
	@Test
	public void testTravelGroups() {
		TravelHelper helper = new TravelHelper();
		assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_SHUTTLE.getTemplate()));
		assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT.getTemplate()));
		assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT_THEED.getTemplate()));
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
		for (Terrain terrain : terrains) {
			assertEquals(100, helper.getTravelFee(terrain, terrain));
		}
		assertEquals(1250, helper.getTravelFee(Terrain.DATHOMIR, Terrain.CORELLIA));
		assertEquals(2000, helper.getTravelFee(Terrain.CORELLIA, Terrain.DATHOMIR));
		assertEquals(4000, helper.getTravelFee(Terrain.NABOO, Terrain.ENDOR));
		assertEquals(500, helper.getTravelFee(Terrain.TATOOINE, Terrain.NABOO));
		assertFalse(helper.isValidRoute(Terrain.TATOOINE, Terrain.YAVIN4));
		assertTrue(helper.isValidRoute(Terrain.TATOOINE, Terrain.LOK));
		assertTrue(helper.isValidRoute(Terrain.TATOOINE, Terrain.NABOO));
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
		assertEquals(starport, helper.getNearestTravelPoint(creature));
		creature.setPosition(Terrain.TATOOINE, 25, 0, 25);
		assertEquals(starport, helper.getNearestTravelPoint(creature));
		creature.setPosition(Terrain.TATOOINE, 500, 0, 500);
		assertEquals(starport, helper.getNearestTravelPoint(creature));
		creature.setPosition(Terrain.TATOOINE, -25, 0, -25);
		assertEquals(shuttleport, helper.getNearestTravelPoint(creature));
		creature.setPosition(Terrain.TATOOINE, -75, 0, -75);
		assertEquals(shuttleport, helper.getNearestTravelPoint(creature));
	}
	
	@Test
	public void testNoInvalidRoutesTatooine() {
		TravelHelper helper = new TravelHelper();
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setPosition(Terrain.TATOOINE, 3500, 5, -4800);
		List<TravelPoint> destinations = helper.getAvailableTravelPoints(creature, Terrain.YAVIN4);
		assertEquals(0, destinations.size());
		destinations = helper.getAvailableTravelPoints(creature, Terrain.LOK);
		assertEquals(1, destinations.size());
		assertEquals("Nym's Stronghold" , destinations.get(0).getName());
	}
	
	@Test
	public void testNoInvalidRoutesCorellia() {
		TravelHelper helper = new TravelHelper();
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setPosition(Terrain.CORELLIA, -75, 0, -4723);
		List<TravelPoint> destinations = helper.getAvailableTravelPoints(creature, Terrain.YAVIN4);
		assertEquals(3, destinations.size());
		assertEquals("Imperial Base", destinations.get(0).getName());
		assertEquals("Labor Outpost", destinations.get(1).getName());
		assertEquals("Mining Outpost", destinations.get(2).getName());
		destinations = helper.getAvailableTravelPoints(creature, Terrain.LOK);
		assertEquals(0, destinations.size());
	}
	
	@Test
	public void testValidTicketCreated() {
		TravelHelper helper = new TravelHelper();
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setPosition(Terrain.YAVIN4, 4054, 0, -6216);
		TravelPoint imperialBase = helper.getDestinationPoint(Terrain.YAVIN4, "Imperial Base");
		TravelPoint coronet = helper.getDestinationPoint(Terrain.CORELLIA, "Coronet Starport");
		helper.grantTicket(imperialBase, coronet, creature);
		assertEquals(imperialBase, helper.getNearestTravelPoint(creature));
		assertEquals(1, creature.getSlottedObject("inventory").getContainedObjects().size());
		List<TangibleObject> tickets = helper.getTickets(creature);
		assertEquals(1, tickets.size());
		
	}
	
}
