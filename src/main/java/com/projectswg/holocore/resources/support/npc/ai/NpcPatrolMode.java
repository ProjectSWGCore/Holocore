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
import com.projectswg.holocore.resources.support.data.server_info.loader.NpcPatrolRouteLoader.PatrolType;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner.ResolvedPatrolWaypoint;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;

import java.util.*;

/**
 * AI object that patrols the specified route
 */
public class NpcPatrolMode extends NpcMode {
	
	private final List<ResolvedPatrolWaypoint> waypoints;
	private final PatrolType patrolType;
	private final Queue<Runnable> plannedRoute;
	
	public NpcPatrolMode(List<ResolvedPatrolWaypoint> waypoints) {
		this.waypoints = new ArrayList<>(waypoints);
		this.patrolType = !waypoints.isEmpty() ? waypoints.get(0).getPatrolType() : PatrolType.LOOP;
		this.plannedRoute = new LinkedList<>();
	}
	
	@Override
	public void act() {
		if (isRooted()) {
			queueNextLoop(1000);
			return;
		}
		if (plannedRoute.isEmpty())
			createPlannedRoute();
		
		Runnable nextAction = plannedRoute.poll();
		if (nextAction != null)
			nextAction.run();
		
		queueNextLoop(1000);
	}
	
	private void createPlannedRoute() {
		Location prevLocation = getAI().getLocation();
		SWGObject prevParent = getAI().getParent();
		for (ResolvedPatrolWaypoint waypoint : waypoints) {
			appendPlannedRouteWaypoint(prevParent, prevLocation, waypoint);
			prevParent = waypoint.getParent();
			prevLocation = waypoint.getLocation();
		}
		if (patrolType == PatrolType.FLIP) {
			List<ResolvedPatrolWaypoint> waypointsReverse = new ArrayList<>(waypoints);
			Collections.reverse(waypointsReverse);
			for (ResolvedPatrolWaypoint waypoint : waypointsReverse) {
				appendPlannedRouteWaypoint(prevParent, prevLocation, waypoint);
				prevParent = waypoint.getParent();
				prevLocation = waypoint.getLocation();
			}
		}
	}
	
	private void appendPlannedRouteWaypoint(SWGObject prevParent, Location prevLocation, ResolvedPatrolWaypoint waypoint) {
		if (prevParent == waypoint.getParent()) {
			Queue<Location> route = AINavigationSupport.navigateTo(prevLocation, waypoint.getLocation(), getWalkSpeed());
			while (!route.isEmpty()) {
				Location l = route.poll();
				assert l != null;
				addToPlannedRoute(prevParent, l);
			}
		} else {
			// Simple teleport to the location within/out of the cell
			addToPlannedRoute(waypoint.getParent(), waypoint.getLocation());
		}
		for (int i = 0; i < waypoint.getDelay(); i++) {
			addNopToPlannedRoute();
		}
	}
	
	private void addToPlannedRoute(SWGObject parent, Location location) {
		plannedRoute.add(() -> walkTo(parent, location));
	}
	
	private void addNopToPlannedRoute() {
		plannedRoute.add(() -> {});
	}
	
}
