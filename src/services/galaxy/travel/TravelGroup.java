/***********************************************************************************
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import resources.Location;
import resources.Posture;
import resources.Terrain;
import resources.TravelPoint;
import resources.objects.creature.CreatureObject;

public class TravelGroup implements Runnable {
	
	private final Set<TravelPoint> points;
	private final Map<Terrain, Set<TravelPoint>> terrainToPoint;
	private final AtomicLong timeRemaining;
	private final String template;
	private final long landTime;
	private final long groundTime;
	private final long airTime;
	private final boolean starport;
	private ShuttleStatus status;
	
	public TravelGroup(String template, long landTime, long groundTime, long airTime) {
		this.points = new HashSet<>();
		this.terrainToPoint = new HashMap<>();
		this.timeRemaining = new AtomicLong(airTime);
		this.template = template;
		this.landTime = landTime + 10000;
		this.groundTime = groundTime;
		this.airTime = airTime;
		this.starport = template.endsWith("shared_player_transport.iff");
		this.status = ShuttleStatus.GROUNDED;
	}
	
	public void addTravelPoint(TravelPoint point) {
		synchronized (points) {
			points.add(point);
		}
		synchronized (terrainToPoint) {
			Terrain t = point.getLocation().getTerrain();
			Set<TravelPoint> s = terrainToPoint.get(t);
			if (s == null) {
				s = new HashSet<>();
				terrainToPoint.put(t, s);
			}
			s.add(point);
		}
	}
	
	public void getPointsForTerrain(Collection<TravelPoint> points, TravelPoint nearest, Terrain to) {
		synchronized (terrainToPoint) {
			Set<TravelPoint> set = terrainToPoint.get(to);
			if (set == null)
				return;
			if (nearest.getLocation().getTerrain() == to)
				points.addAll(set);
			else if (nearest.isStarport() && starport)
				points.addAll(set);
		}
	}
	
	public TravelPoint getNearestPoint(Location l) {
		synchronized (terrainToPoint) {
			Set<TravelPoint> set = terrainToPoint.get(l.getTerrain());
			if (set == null)
				return null;
			TravelPoint nearest = null;
			double dist = Double.MAX_VALUE;
			for (TravelPoint tp : set) {
				if (tp.getLocation().distanceTo(l) < dist) {
					nearest = tp;
					dist = tp.getLocation().distanceTo(l);
				}
			}
			return nearest;
		}
	}
	
	public TravelPoint getDestination(Terrain t, String destination) {
		synchronized (terrainToPoint) {
			Set<TravelPoint> set = terrainToPoint.get(t);
			if (set == null)
				return null;
			for (TravelPoint tp : set) {
				if (tp.getName().equals(destination))
					return tp;
			}
			return null;
		}
	}
	
	public int getTimeRemaining() {
		return timeRemaining.intValue();
	}
	
	public ShuttleStatus getStatus() {
		return status;
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				// GROUNDED
				status = ShuttleStatus.GROUNDED;
				Thread.sleep(groundTime);
				
				// LEAVING
				status = ShuttleStatus.LEAVING;
				updateShuttlePostures(false);
				Thread.sleep(landTime);
				
				// AWAY
				status = ShuttleStatus.AWAY;
				for (int timeElapsed = 0; timeElapsed < airTime / 1000; timeElapsed++) {
					Thread.sleep(1000);	// Sleep for a second
					timeRemaining.decrementAndGet();
				}
				timeRemaining.set(airTime / 1000);	// Reset the timer
				
				// LANDING
				status = ShuttleStatus.LANDING;
				updateShuttlePostures(true);
				Thread.sleep(landTime);
			}
		} catch (InterruptedException e) {
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void updateShuttlePostures(boolean landed) {
		synchronized (points) {
			for (TravelPoint tp : points) {
				CreatureObject shuttle = tp.getShuttle();
				
				if (shuttle == null || !shuttle.getTemplate().equals(template))	// This TravelPoint has no associated shuttle
					continue;	// Continue with the next TravelPoint
				
				shuttle.setPosture(landed ? Posture.UPRIGHT : Posture.PRONE);
			}
		}
	}
	
	public enum ShuttleStatus {
		LANDING,
		GROUNDED,
		LEAVING,
		AWAY
	}
	
}
