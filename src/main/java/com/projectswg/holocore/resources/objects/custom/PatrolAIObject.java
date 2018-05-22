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
package com.projectswg.holocore.resources.objects.custom;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.object.MoveObjectIntent;
import com.projectswg.holocore.resources.server_info.loader.npc.NpcPatrolRouteLoader.PatrolType;
import com.projectswg.holocore.resources.spawn.Spawner.ResolvedPatrolWaypoint;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI object that patrols the specified route
 */
public class PatrolAIObject extends AIObject {
	
	private final AtomicInteger updateCounter;
	private final PatrolRoute route;
	
	public PatrolAIObject(long objectId) {
		super(objectId);
		this.updateCounter = new AtomicInteger(0);
		this.route = new PatrolRoute();
	}
	
	public void setPatrolWaypoints(List<ResolvedPatrolWaypoint> waypoints) {
		if (!waypoints.isEmpty())
			route.setPatrolType(waypoints.get(0).getPatrolType());
		route.updateWaypoints(waypoints);
	}
	
	@Override
	protected void aiInitialize() {
		super.aiInitialize();
		setSchedulerProperties((int) (Math.random()*1000), 1000, TimeUnit.MILLISECONDS);
	}
	
	@Override
	protected void aiLoop() {
		if (isInCombat() || !canAiMove() || !hasNearbyPlayers())
			return;
		
		double speed = getMovementPercent() * getMovementScale() * getWalkSpeed();
		route.move(speed);
		ResolvedPatrolWaypoint waypoint = route.getPreviousWaypoint();
		Location loc = route.getCurrentLocation();
		MoveObjectIntent.broadcast(this, waypoint.getParent(), loc, speed, updateCounter.getAndIncrement());
	}
	
	public static class PatrolRoute {
		
		private final Object mutex;
		
		private Location currentLocation;
		private ResolvedPatrolWaypoint previousWaypoint;
		
		// Movement calculations
		private ResolvedPatrolWaypoint [] waypoints;
		private double [] waypointDistances;
		private PatrolType patrolType;
		private double routeDistance;
		private double distanceTravelled;
		
		public PatrolRoute() {
			this.mutex = new Object();
			
			this.currentLocation = null;
			this.previousWaypoint = null;
			
			this.waypoints = new ResolvedPatrolWaypoint[0];
			this.waypointDistances = new double[0];
			this.patrolType = PatrolType.LOOP;
			this.routeDistance = 0;
			this.distanceTravelled = 0;
		}
		
		public void updateWaypoints(List<ResolvedPatrolWaypoint> updatedWaypoints) {
			synchronized (mutex) {
				double distance = 0;
				waypoints = new ResolvedPatrolWaypoint[updatedWaypoints.size()];
				waypointDistances = new double[updatedWaypoints.size()];
				
				waypoints[0] = updatedWaypoints.get(0);
				waypointDistances[0] = 0;
				for (int i = 1; i < waypoints.length; i++) {
					waypoints[i] = updatedWaypoints.get(i);
					waypointDistances[i] = waypoints[i-1].getLocation().distanceTo(waypoints[i].getLocation());
					distance += waypointDistances[i];
				}
				this.routeDistance = distance;
			}
		}
		
		public void setPatrolType(PatrolType patrolType) {
			this.patrolType = patrolType;
		}
		
		public void move(double speed) {
			synchronized (mutex) {
				distanceTravelled += speed;
				if (patrolType == PatrolType.FLIP)
					distanceTravelled %= routeDistance * 2;
				else
					distanceTravelled %= routeDistance;
				update();
			}
		}
		
		public Location getCurrentLocation() {
			return currentLocation;
		}
		
		public ResolvedPatrolWaypoint getPreviousWaypoint() {
			return previousWaypoint;
		}
		
		private void update() {
			switch (waypoints.length) {
				case 0:
					return;
				case 1:
					updateValues(getFirstWaypoint(), 0);
					return;
			}
			switch (patrolType) {
				case LOOP:
				default:
					updateLocationLoop();
					break;
				case FLIP:
					updateLocationFlip();
					break;
			}
		}
		
		private void updateLocationLoop() {
			if (distanceTravelled == 0) {
				updateValues(getFirstWaypoint(), 0);
				return;
			}
			
			double distance = 0;
			
			for (int i = 1; i < waypoints.length; i++) {
				double waypointDistance = waypointDistances[i];
				if (distanceTravelled <= distance+waypointDistance) {
					updateValues(interpolate(getWaypoint(i-1), getWaypoint(i), (distanceTravelled - distance) / waypointDistance), i-1);
					return;
				}
				distance += waypointDistance;
			}
			
			updateValues(getLastWaypoint(), waypoints.length-1);
		}
		
		private void updateLocationFlip() {
			// Shortcut
			if (distanceTravelled <= routeDistance) {
				updateLocationLoop();
				return;
			}
			
			double distance = routeDistance;
			
			for (int i = waypoints.length-2; i >= 0; i--) {
				double waypointDistance = waypointDistances[i+1];
				if (distanceTravelled < distance + waypointDistance) {
					updateValues(interpolate(getWaypoint(i+1), getWaypoint(i), (distanceTravelled - distance) / waypointDistance), i+1);
					return;
				}
				distance += waypointDistance;
			}
			
			updateValues(getFirstWaypoint(), 0);
		}
		
		private Location getFirstWaypoint() {
			return getWaypoint(0);
		}
		
		private Location getLastWaypoint() {
			return getWaypoint(waypoints.length-1);
		}
		
		private Location getWaypoint(int index) {
			return waypoints[index].getLocation();
		}
		
		private void updateValues(Location loc, int lastIndex) {
			this.currentLocation = loc;
			this.previousWaypoint = waypoints[lastIndex];
		}
		
		private static Location interpolate(Location l1, Location l2, double percentage) {
			return Location.builder()
					.setTerrain(l1.getTerrain())
					.setX(l1.getX() + (l2.getX()-l1.getX())*percentage)
					.setY(l1.getY() + (l2.getY()-l1.getY())*percentage)
					.setZ(l1.getZ() + (l2.getZ()-l1.getZ())*percentage)
					.setHeading(Math.toDegrees(Math.atan2(l2.getX()-l1.getX(), l2.getZ()-l1.getZ())))
					.build();
		}
		
	}
	
}
