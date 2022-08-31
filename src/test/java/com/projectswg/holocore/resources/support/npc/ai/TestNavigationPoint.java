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

package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TestNavigationPoint extends TestRunnerNoIntents {
	
	private static final double SPEED = 1.5;
	
	@Test
	public void testDistanceTo() {
		NavigationPoint src = NavigationPoint.at(null, Location.builder().setPosition(0, 0, 0).build(), 0);
		NavigationPoint dst = NavigationPoint.at(null, Location.builder().setPosition(0, 0, 10).build(), 0);
		
		assertEquals(10, src.distanceTo(dst), 1E-7);
		assertEquals(10, dst.distanceTo(src), 1E-7);
		assertEquals(5, src.distanceTo(null, Location.builder().setPosition(5, 0, 0).build()), 1E-7);
	}
	
	@Test
	public void testDirect() {
		Location start = location(0, 0, 0);
		Location end = location(10, 0, 0);
		List<NavigationPoint> route = from(null, start, end);
		
		assertEquals(route, NavigationPoint.from(null, start, end, SPEED));
	}
	
	@Test
	public void testIntoBuilding() {
		BuildingObject buio = (BuildingObject) ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		buio.setPosition(Terrain.TATOOINE, 10, 0, 0);
		buio.setHeading(45);
		buio.populateCells();
		
		Location start = location(0, 0, 0);
		Location portal = buildPortalLocation(buio.getCellByNumber(1).getPortalTo(null));
		Location worldPortal = Location.builder(portal).translateLocation(buio.getLocation()).build();
		Location end = location(0, 0, 0);
		
		List<NavigationPoint> route = new ArrayList<>();
		route.addAll(from(null, start, worldPortal));
		route.addAll(from(buio.getCellByNumber(1), portal, end));
		
		assertEquals(route, NavigationPoint.from(null, start, buio.getCellByNumber(1), end, SPEED));
	}
	
	@Test
	public void testOutOfBuilding() {
		BuildingObject buio = (BuildingObject) ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		buio.setPosition(Terrain.TATOOINE, 10, 0, 0);
		buio.setHeading(45);
		buio.populateCells();
		
		Location start = location(0, 0, 0);
		Location portal = buildPortalLocation(buio.getCellByNumber(1).getPortalTo(null));
		Location worldPortal = Location.builder(portal).translateLocation(buio.getLocation()).build();
		Location end = location(0, 0, 0);
		
		List<NavigationPoint> route = new ArrayList<>();
		route.addAll(from(buio.getCellByNumber(1), end, portal));
		route.addAll(from(null, worldPortal, start));
		
		assertEquals(route, NavigationPoint.from(buio.getCellByNumber(1), end, null, start, SPEED));
	}
	
	@Test
	public void testIntoWithinBuilding() {
		BuildingObject buio = (BuildingObject) ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		buio.setPosition(Terrain.TATOOINE, -10, 0, 0);
		buio.setHeading(45);
		buio.populateCells();
		
		Location start = location(0, 0, 0);
		Location portal1 = buildPortalLocation(buio.getCellByNumber(1).getPortalTo(null));
		Location portal2 = buildPortalLocation(buio.getCellByNumber(2).getPortalTo(buio.getCellByNumber(1)));
		Location worldPortal = Location.builder(portal1).translateLocation(buio.getLocation()).build();
		Location end = location(0, 0, 0);
		
		List<NavigationPoint> route = new ArrayList<>();
		route.addAll(from(null, start, worldPortal));
		route.addAll(from(buio.getCellByNumber(1), portal1, portal2));
		route.addAll(from(buio.getCellByNumber(2), portal2, end));
		
		assertEquals(route, NavigationPoint.from(null, start, buio.getCellByNumber(2), end, SPEED));
	}
	
	@Test
	public void testIntoWithinBuilding2() {
		BuildingObject buio = (BuildingObject) ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		buio.setPosition(Terrain.TATOOINE, -10, 0, 0);
		buio.setHeading(45);
		buio.populateCells();
		
		Location start = location(0, 0, 0);
		Location portal1 = buildPortalLocation(buio.getCellByNumber(1).getPortalTo(null));
		Location portal2 = buildPortalLocation(buio.getCellByNumber(1).getPortalTo(buio.getCellByNumber(2)));
		Location portal3 = buildPortalLocation(buio.getCellByNumber(2).getPortalTo(buio.getCellByNumber(3)));
		Location worldPortal = Location.builder(portal1).translateLocation(buio.getLocation()).build();
		Location end = location(0, 0, 0);
		
		List<NavigationPoint> route = new ArrayList<>();
		route.addAll(from(null, start, worldPortal));
		route.addAll(from(buio.getCellByNumber(1), portal1, portal2));
		route.addAll(from(buio.getCellByNumber(2), portal2, portal3));
		route.addAll(from(buio.getCellByNumber(3), portal3, end));
		
		assertEquals(route, NavigationPoint.from(null, start, buio.getCellByNumber(3), end, SPEED));
	}
	
	@Test
	public void testOutOfWithinBuilding() {
		BuildingObject buio = (BuildingObject) ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		buio.setPosition(Terrain.TATOOINE, -10, 0, 0);
		buio.setHeading(45);
		buio.populateCells();
		
		Location start = location(0, 0, 0);
		Location portal1 = buildPortalLocation(buio.getCellByNumber(1).getPortalTo(null));
		Location portal2 = buildPortalLocation(buio.getCellByNumber(2).getPortalTo(buio.getCellByNumber(1)));
		Location worldPortal = Location.builder(portal1).translateLocation(buio.getLocation()).build();
		Location end = location(0, 0, 0);
		
		List<NavigationPoint> route = new ArrayList<>();
		route.addAll(from(buio.getCellByNumber(2), end, portal2));
		route.addAll(from(buio.getCellByNumber(1), portal2, portal1));
		route.addAll(from(null, worldPortal, start));
		
		assertEquals(route, NavigationPoint.from(buio.getCellByNumber(2), end, null, start, SPEED));
	}
	
	@Test
	public void testWithinBuilding() {
		BuildingObject buio = (BuildingObject) ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		buio.setPosition(Terrain.TATOOINE, -10, 0, 0);
		buio.setHeading(270);
		buio.populateCells();
		
		Location start = location(5, 0, 5);
		Location portal = buildPortalLocation(buio.getCellByNumber(2).getPortalTo(buio.getCellByNumber(1)));
		Location end = location(-5, 0, -5);
		
		List<NavigationPoint> route = new ArrayList<>();
		route.addAll(from(buio.getCellByNumber(1), start, portal));
		route.addAll(from(buio.getCellByNumber(2), portal, end));
		
		assertEquals(route, NavigationPoint.from(buio.getCellByNumber(1), start, buio.getCellByNumber(2), end, SPEED));
	}
	
	@Test
	public void testWithinBuildingSimple() {
		BuildingObject buio = (BuildingObject) ObjectCreator.createObjectFromTemplate(4, "object/building/player/shared_player_house_tatooine_small_style_01.iff");
		buio.setPosition(Terrain.TATOOINE, -10, 0, 0);
		buio.setHeading(270);
		buio.populateCells();
		
		Location start = location(5, 0, 5);
		Location end = location(-5, 0, -5);
		
		assertEquals(from(buio.getCellByNumber(1), start, end), NavigationPoint.from(buio.getCellByNumber(1), start, buio.getCellByNumber(1), end, SPEED));
	}
	
	private static List<NavigationPoint> from(@Nullable SWGObject parent, @NotNull Location source, @NotNull Location destination) {
		return NavigationPoint.from(parent, source, destination, SPEED);
	}
	
	private static Location buildPortalLocation(Portal portal) {
		return Location.builder()
				.setX(average(portal.getFrame1().getX(), portal.getFrame2().getX()))
				.setY(average(portal.getFrame1().getY(), portal.getFrame2().getY()))
				.setZ(average(portal.getFrame1().getZ(), portal.getFrame2().getZ()))
				.setTerrain(portal.getCell1().getTerrain())
				.build();
	}
	
	private static double average(double x, double y) {
		return (x+y) / 2;
	}
	
	private static Location location(double x, double y, double z) {
		return Location.builder().setPosition(x, y, z).setTerrain(Terrain.TATOOINE).build();
	}
	
}
