/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.resources.gameplay.world.travel

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.objects.SpecificObject
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerNoIntents
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestTravelHelper : TestRunnerNoIntents() {
	@Test
	fun testTravelGroups() {
		val helper = TravelHelper()
		Assertions.assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_SHUTTLE.template))
		Assertions.assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT.template))
		Assertions.assertNotNull(helper.getTravelGroup(SpecificObject.SO_TRANSPORT_STARPORT_THEED.template))
	}

	@Test
	fun testRoutesLoaded() {
		val terrains = arrayOf(
			Terrain.CORELLIA, Terrain.DANTOOINE, Terrain.DATHOMIR, Terrain.ENDOR, Terrain.LOK, Terrain.NABOO, Terrain.RORI, Terrain.TALUS, Terrain.TATOOINE, Terrain.YAVIN4, Terrain.MUSTAFAR
		)
		val helper = TravelHelper()
		for (terrain in terrains) {
			Assertions.assertEquals(100, helper.getTravelFee(terrain, terrain))
		}
		Assertions.assertEquals(1250, helper.getTravelFee(Terrain.DATHOMIR, Terrain.CORELLIA))
		Assertions.assertEquals(2000, helper.getTravelFee(Terrain.CORELLIA, Terrain.DATHOMIR))
		Assertions.assertEquals(4000, helper.getTravelFee(Terrain.NABOO, Terrain.ENDOR))
		Assertions.assertEquals(500, helper.getTravelFee(Terrain.TATOOINE, Terrain.NABOO))
		Assertions.assertFalse(helper.isValidRoute(Terrain.TATOOINE, Terrain.YAVIN4))
		Assertions.assertTrue(helper.isValidRoute(Terrain.TATOOINE, Terrain.LOK))
		Assertions.assertTrue(helper.isValidRoute(Terrain.TATOOINE, Terrain.NABOO))
	}

	@Test
	fun `test filter local travel point`() {
		val helper = TravelHelper()
		val creature = GenericCreatureObject(1)
		creature.setPosition(Terrain.TATOOINE, 3414.0, 5.0, -4658.0)
		val travelPointsMosEisleyShuttle = helper.getTravelPoints(creature, Terrain.TATOOINE, 1.0)
		Assertions.assertEquals(0, travelPointsMosEisleyShuttle.count { it.name == "Mos Eisley Shuttleport" })
	}

	@Test
	fun testPointsOnPlanet() {
		val helper = TravelHelper()
		val starport = TravelPoint("starport", Location(50.0, 0.0, 50.0, Terrain.TATOOINE), null, true)
		val shuttleport = TravelPoint("shuttleport", Location(-50.0, 0.0, -50.0, Terrain.TATOOINE), null, false)
		val outOfRange = TravelPoint("outOfRange", Location(1000.0, 0.0, 1000.0, Terrain.TATOOINE), null, true)
		helper.addTravelPoint(starport)
		helper.addTravelPoint(shuttleport)
		helper.addTravelPoint(outOfRange)
		val creature = GenericCreatureObject(1)
		creature.setPosition(Terrain.TATOOINE, 75.0, 0.0, 75.0)
		Assertions.assertEquals(starport, helper.getNearestTravelPoint(creature))
		creature.setPosition(Terrain.TATOOINE, 25.0, 0.0, 25.0)
		Assertions.assertEquals(starport, helper.getNearestTravelPoint(creature))
		creature.setPosition(Terrain.TATOOINE, 100.0, 0.0, 100.0)
		Assertions.assertEquals(starport, helper.getNearestTravelPoint(creature))
		creature.setPosition(Terrain.TATOOINE, -40.0, 0.0, -40.0)
		Assertions.assertEquals(shuttleport, helper.getNearestTravelPoint(creature))
		creature.setPosition(Terrain.TATOOINE, -60.0, 0.0, -60.0)
		Assertions.assertEquals(shuttleport, helper.getNearestTravelPoint(creature))
	}

	@Test
	fun testNoInvalidRoutesTatooine() {
		val helper = TravelHelper()
		val creature = GenericCreatureObject(1)
		creature.setPosition(Terrain.TATOOINE, 3565.0, 5.0, -4805.0)
		var destinations: List<TravelPoint> = helper.getAvailableTravelPoints(creature, Terrain.YAVIN4)
		Assertions.assertEquals(0, destinations.size)
		destinations = helper.getAvailableTravelPoints(creature, Terrain.LOK)
		Assertions.assertEquals(1, destinations.size)
		Assertions.assertEquals("Nym's Stronghold", destinations[0].name)
	}

	@Test
	fun testNoInvalidRoutesCorellia() {
		val helper = TravelHelper()
		val creature = GenericCreatureObject(1)
		creature.setPosition(Terrain.CORELLIA, -75.0, 0.0, -4723.0)
		var destinations: List<TravelPoint> = helper.getAvailableTravelPoints(creature, Terrain.YAVIN4)
		Assertions.assertEquals(3, destinations.size)
		Assertions.assertEquals("Imperial Base", destinations[0].name)
		Assertions.assertEquals("Labor Outpost", destinations[1].name)
		Assertions.assertEquals("Mining Outpost", destinations[2].name)
		destinations = helper.getAvailableTravelPoints(creature, Terrain.LOK)
		Assertions.assertEquals(0, destinations.size)
	}

	@Test
	fun testValidTicketCreated() {
		val helper = TravelHelper()
		val creature = GenericCreatureObject(1)
		creature.setPosition(Terrain.YAVIN4, 4054.0, 0.0, -6216.0)
		val imperialBase = helper.getDestinationPoint(Terrain.YAVIN4, "Imperial Base")
		val coronet = helper.getDestinationPoint(Terrain.CORELLIA, "Coronet Starport")
		helper.grantTicket(imperialBase!!, coronet!!, creature)
		Assertions.assertEquals(imperialBase, helper.getNearestTravelPoint(creature))
		Assertions.assertEquals(1, creature.getSlottedObject("inventory").containedObjects.size)
		val tickets = helper.getTickets(creature)
		Assertions.assertEquals(1, tickets.size)
	}
}
