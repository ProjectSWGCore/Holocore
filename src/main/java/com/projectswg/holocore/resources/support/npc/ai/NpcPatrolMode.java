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
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject.ScheduledMode;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI object that patrols the specified route
 */
public class NpcPatrolMode extends NpcMode {
	
	private final List<NavigationPoint> waypoints;
	
	private int movementIndex;
	
	public NpcPatrolMode(AIObject obj, List<ResolvedPatrolWaypoint> waypoints) {
		super(obj, ScheduledMode.DEFAULT);
		this.waypoints = new ArrayList<>(waypoints.size());
		
		this.movementIndex = 0;
		buildRoute(waypoints);
	}
	
	@Override
	public void act() {
		if (isRooted() || waypoints.isEmpty()) {
			queueNextLoop(1000);
			return;
		}
		waypoints.get(movementIndex).move(getAI());
		movementIndex = (movementIndex + 1) % waypoints.size();
		
		queueNextLoop(1000);
	}
	
	private void buildRoute(List<ResolvedPatrolWaypoint> waypoints) {
		if (waypoints.isEmpty())
			return;
		PatrolType type = waypoints.get(0).getPatrolType();
		if (type == PatrolType.LOOP) // Connect the beginning and end
			waypoints.add(0, last(waypoints));
		
		for (ResolvedPatrolWaypoint waypoint : waypoints) {
			appendRoutePoint(waypoint);
		}
		
		if (type == PatrolType.FLIP) {
			List<ResolvedPatrolWaypoint> waypointsReverse = new ArrayList<>(waypoints);
			Collections.reverse(waypointsReverse);
			for (ResolvedPatrolWaypoint waypoint : waypointsReverse) {
				appendRoutePoint(waypoint);
			}
		}
	}
	
	private void appendRoutePoint(ResolvedPatrolWaypoint waypoint) {
		NavigationPoint prev = waypoints.isEmpty() ? null : waypoints.get(waypoints.size()-1);
		if (prev == null) {
			waypoints.add(new NavigationPoint(waypoint.getParent(), waypoint.getLocation(), getWalkSpeed()));
		} else {
			if (prev.getLocation().equals(waypoint.getLocation()) && prev.getParent() == waypoint.getParent())
				return;
			waypoints.addAll(NavigationPoint.from(prev.getParent(), prev.getLocation(), waypoint.getParent(), waypoint.getLocation(), getWalkSpeed()));
		}
		waypoints.addAll(NavigationPoint.nop(last(waypoints), (int) waypoint.getDelay()));
	}
	
	private static <T> T last(List<T> list) {
		return list.isEmpty() ? null : list.get(list.size()-1);
	}
	
}
