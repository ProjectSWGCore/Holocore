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

import com.projectswg.holocore.intents.support.npc.ai.CompileNpcMovementIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.NpcPatrolRouteLoader.PatrolType;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner.ResolvedPatrolWaypoint;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI object that patrols the specified route
 */
public class NpcPatrolMode extends NpcMode {
	
	private final List<NavigationPoint> waypoints;
	
	public NpcPatrolMode(AIObject obj, List<ResolvedPatrolWaypoint> waypoints) {
		super(obj);
		this.waypoints = new ArrayList<>(waypoints.size());
		
		if (!waypoints.isEmpty()) {
			waypoints = new ArrayList<>(waypoints);
			if (waypoints.get(0).getPatrolType() == PatrolType.LOOP) {
				waypoints.add(0, last(waypoints));
			} else if (waypoints.get(0).getPatrolType() == PatrolType.FLIP) {
				List<ResolvedPatrolWaypoint> reversed = new ArrayList<>(waypoints);
				Collections.reverse(reversed);
				waypoints.addAll(reversed);
			} else {
				assert false;
			}
		}
		for (ResolvedPatrolWaypoint waypoint : waypoints) {
			NavigationPoint point = NavigationPoint.at(waypoint.getParent(), waypoint.getLocation(), getWalkSpeed());
			this.waypoints.add(point);
			this.waypoints.addAll(NavigationPoint.nop(point, (int) waypoint.getDelay()));
		}
	}
	
	@Override
	public void onModeStart() {
		if (!waypoints.isEmpty()) {
			int index = 0;
			double closestDistance = waypoints.get(0).distanceTo(getAI());
			for (int i = 1; i < waypoints.size(); i++) {
				double distance = waypoints.get(i).distanceTo(getAI());
				if (distance < closestDistance) {
					closestDistance = distance;
					index = i;
				}
			}
			List<NavigationPoint> rearranged = new ArrayList<>(waypoints.size());
			rearranged.addAll(waypoints.subList(index, waypoints.size()));
			rearranged.addAll(waypoints.subList(0, index));
			
			waypoints.clear();
			waypoints.addAll(rearranged);
		}
		CompileNpcMovementIntent.broadcast(getAI(), waypoints, NavigationRouteType.LOOP, getWalkSpeed());
	}
	
	@Override
	public void act() {
		
	}
	
	private static <T> T last(List<T> list) {
		return list.isEmpty() ? null : list.get(list.size()-1);
	}
	
}
