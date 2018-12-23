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
import com.projectswg.holocore.intents.support.npc.ai.CompileNpcMovementIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.NpcPatrolRouteLoader.PatrolType;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner.ResolvedPatrolWaypoint;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI object that patrols the specified route
 */
public class NpcPatrolMode extends NpcMode {
	
	private final List<NavigationPoint> waypoints;
	
	public NpcPatrolMode(@NotNull AIObject obj, @NotNull List<ResolvedPatrolWaypoint> waypoints) {
		super(obj);
		waypoints = new ArrayList<>(waypoints);
		this.waypoints = new ArrayList<>(waypoints.size());
		
		if (!waypoints.isEmpty() && waypoints.get(0).getPatrolType() == PatrolType.FLIP) {
			List<ResolvedPatrolWaypoint> reversed = new ArrayList<>(waypoints);
			Collections.reverse(reversed);
			waypoints.addAll(reversed);
		} else if (!waypoints.isEmpty() && waypoints.get(0).getPatrolType() == PatrolType.LOOP) {
			waypoints.add(waypoints.get(0));
		}
		
		for (ResolvedPatrolWaypoint waypoint : waypoints) {
			NavigationPoint point = NavigationPoint.at(waypoint.getParent(), waypoint.getLocation(), getWalkSpeed());
			this.waypoints.add(point);
			this.waypoints.addAll(NavigationPoint.nop(point, (int) waypoint.getDelay()));
		}
	}
	
	@Override
	public void onModeStart() {
		List<NavigationPoint> compiledWaypoints;
		if (!waypoints.isEmpty()) {
			int index = 0;
			double closestDistance = waypoints.get(0).distanceTo(getAI());
			for (int i = 1; i < waypoints.size(); i++) {
				if (waypoints.get(i).isNoOperation())
					continue;
				
				double distance = waypoints.get(i).distanceTo(getAI());
				if (distance < closestDistance) {
					closestDistance = distance;
					index = i;
				}
			}
			compiledWaypoints = new ArrayList<>(waypoints.size());
			for (int i = index; i < waypoints.size(); i++) {
				compiledWaypoints.add(waypoints.get(i));
			}
			for (int i = 0; i < index; i++) {
				compiledWaypoints.add(waypoints.get(i));
			}
		} else {
			compiledWaypoints = waypoints;
		}
		CompileNpcMovementIntent.broadcast(getAI(), compiledWaypoints, NavigationRouteType.LOOP, getWalkSpeed());
	}
	
	@Override
	public void act() {
		
	}
	
}
