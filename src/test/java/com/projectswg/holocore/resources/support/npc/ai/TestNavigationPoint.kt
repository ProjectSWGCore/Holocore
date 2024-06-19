/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.npc.ai

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal
import com.projectswg.holocore.test.runners.TestRunnerNoIntents
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestNavigationPoint : TestRunnerNoIntents() {

	@Test
	fun testDistanceTo() {
		val src = NavigationPoint.at(null, Location.builder().setPosition(0.0, 0.0, 0.0).build(), 0.0)
		val dst = NavigationPoint.at(null, Location.builder().setPosition(0.0, 0.0, 10.0).build(), 0.0)

		assertEquals(10.0, src.distanceTo(dst), 1E-7)
		assertEquals(10.0, dst.distanceTo(src), 1E-7)
		assertEquals(5.0, src.distanceTo(null, Location.builder().setPosition(5.0, 0.0, 0.0).build()), 1E-7)
	}

	@Test
	fun testDirect() {
		val start = location(0.0, 0.0)
		val end = location(10.0, 0.0)
		val route = from(null, start, end)

		assertEquals(route, NavigationPoint.from(null, start, end, SPEED))
	}

	@Test
	fun testIntoBuilding() {
		val buio = ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff") as BuildingObject
		buio.setPosition(Terrain.TATOOINE, 10.0, 0.0, 0.0)
		buio.setHeading(45.0)
		buio.populateCells()

		val start = location(0.0, 0.0)
		val portal = buildPortalLocation(buio.getCellByNumber(1)!!.getPortalTo(null))
		val worldPortal = Location.builder(portal).translateLocation(buio.location).build()
		val end = location(0.0, 0.0)

		val route: MutableList<NavigationPoint> = ArrayList()
		route.addAll(from(null, start, worldPortal))
		route.addAll(from(buio.getCellByNumber(1), portal, end))

		assertEquals(route, NavigationPoint.from(null, start, buio.getCellByNumber(1), end, SPEED))
	}

	@Test
	fun testOutOfBuilding() {
		val buio = ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff") as BuildingObject
		buio.setPosition(Terrain.TATOOINE, 10.0, 0.0, 0.0)
		buio.setHeading(45.0)
		buio.populateCells()

		val start = location(0.0, 0.0)
		val portal = buildPortalLocation(buio.getCellByNumber(1)!!.getPortalTo(null))
		val worldPortal = Location.builder(portal).translateLocation(buio.location).build()
		val end = location(0.0, 0.0)

		val route: MutableList<NavigationPoint> = ArrayList()
		route.addAll(from(buio.getCellByNumber(1), end, portal))
		route.addAll(from(null, worldPortal, start))

		assertEquals(route, NavigationPoint.from(buio.getCellByNumber(1), end, null, start, SPEED))
	}

	@Test
	fun testIntoWithinBuilding() {
		val buio = ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff") as BuildingObject
		buio.setPosition(Terrain.TATOOINE, -10.0, 0.0, 0.0)
		buio.setHeading(45.0)
		buio.populateCells()

		val start = location(0.0, 0.0)
		val portal1 = buildPortalLocation(buio.getCellByNumber(1)!!.getPortalTo(null))
		val portal2 = buildPortalLocation(buio.getCellByNumber(2)!!.getPortalTo(buio.getCellByNumber(1)))
		val worldPortal = Location.builder(portal1).translateLocation(buio.location).build()
		val end = location(0.0, 0.0)

		val route: MutableList<NavigationPoint> = ArrayList()
		route.addAll(from(null, start, worldPortal))
		route.addAll(from(buio.getCellByNumber(1), portal1, portal2))
		route.addAll(from(buio.getCellByNumber(2), portal2, end))

		assertEquals(route, NavigationPoint.from(null, start, buio.getCellByNumber(2), end, SPEED))
	}

	@Test
	fun testIntoWithinBuilding2() {
		val buio = ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff") as BuildingObject
		buio.setPosition(Terrain.TATOOINE, -10.0, 0.0, 0.0)
		buio.setHeading(45.0)
		buio.populateCells()

		val start = location(0.0, 0.0)
		val portal1 = buildPortalLocation(buio.getCellByNumber(1)!!.getPortalTo(null))
		val portal2 = buildPortalLocation(buio.getCellByNumber(1)!!.getPortalTo(buio.getCellByNumber(2)))
		val portal3 = buildPortalLocation(buio.getCellByNumber(2)!!.getPortalTo(buio.getCellByNumber(3)))
		val worldPortal = Location.builder(portal1).translateLocation(buio.location).build()
		val end = location(0.0, 0.0)

		val route: MutableList<NavigationPoint> = ArrayList()
		route.addAll(from(null, start, worldPortal))
		route.addAll(from(buio.getCellByNumber(1), portal1, portal2))
		route.addAll(from(buio.getCellByNumber(2), portal2, portal3))
		route.addAll(from(buio.getCellByNumber(3), portal3, end))

		assertEquals(route, NavigationPoint.from(null, start, buio.getCellByNumber(3), end, SPEED))
	}

	@Test
	fun testOutOfWithinBuilding() {
		val buio = ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff") as BuildingObject
		buio.setPosition(Terrain.TATOOINE, -10.0, 0.0, 0.0)
		buio.setHeading(45.0)
		buio.populateCells()

		val start = location(0.0, 0.0)
		val portal1 = buildPortalLocation(buio.getCellByNumber(1)!!.getPortalTo(null))
		val portal2 = buildPortalLocation(buio.getCellByNumber(2)!!.getPortalTo(buio.getCellByNumber(1)))
		val worldPortal = Location.builder(portal1).translateLocation(buio.location).build()
		val end = location(0.0, 0.0)

		val route: MutableList<NavigationPoint> = ArrayList()
		route.addAll(from(buio.getCellByNumber(2), end, portal2))
		route.addAll(from(buio.getCellByNumber(1), portal2, portal1))
		route.addAll(from(null, worldPortal, start))

		assertEquals(route, NavigationPoint.from(buio.getCellByNumber(2), end, null, start, SPEED))
	}

	@Test
	fun testWithinBuilding() {
		val buio = ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff") as BuildingObject
		buio.setPosition(Terrain.TATOOINE, -10.0, 0.0, 0.0)
		buio.setHeading(270.0)
		buio.populateCells()

		val start = location(5.0, 5.0)
		val portal = buildPortalLocation(buio.getCellByNumber(2)!!.getPortalTo(buio.getCellByNumber(1)))
		val end = location(-5.0, -5.0)

		val route: MutableList<NavigationPoint> = ArrayList()
		route.addAll(from(buio.getCellByNumber(1), start, portal))
		route.addAll(from(buio.getCellByNumber(2), portal, end))

		assertEquals(route, NavigationPoint.from(buio.getCellByNumber(1), start, buio.getCellByNumber(2), end, SPEED))
	}

	@Test
	fun testWithinBuildingSimple() {
		val buio = ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff") as BuildingObject
		buio.setPosition(Terrain.TATOOINE, -10.0, 0.0, 0.0)
		buio.setHeading(270.0)
		buio.populateCells()

		val start = location(5.0, 5.0)
		val end = location(-5.0, -5.0)

		assertEquals(from(buio.getCellByNumber(1), start, end), NavigationPoint.from(buio.getCellByNumber(1), start, buio.getCellByNumber(1), end, SPEED))
	}

	companion object {
		private const val SPEED = 1.5

		private fun from(parent: SWGObject?, source: Location, destination: Location): List<NavigationPoint> {
			return NavigationPoint.from(parent, source, destination, SPEED)
		}

		private fun buildPortalLocation(portal: Portal?): Location {
			return Location.builder().setX(average(portal!!.frame1.x, portal.frame2.x)).setY(average(portal.frame1.y, portal.frame2.y)).setZ(average(portal.frame1.z, portal.frame2.z)).setTerrain(portal.cell1!!.terrain).build()
		}

		private fun average(x: Double, y: Double): Double {
			return (x + y) / 2
		}

		private fun location(x: Double, z: Double): Location {
			return Location.builder().setPosition(x, 0.0, z).setTerrain(Terrain.TATOOINE).build()
		}
	}
}
