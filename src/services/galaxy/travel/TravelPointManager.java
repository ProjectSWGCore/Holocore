/************************************************************************************
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;

public class TravelPointManager {
	
	private final Map<Terrain, List<TravelPoint>> shuttlePoints;
	private final Map<Terrain, List<TravelPoint>> starportPoints;
	
	public TravelPointManager() {
		this.shuttlePoints = new HashMap<>();
		this.starportPoints = new HashMap<>();
	}
	
	public void addTravelPoint(TravelPoint point) {
		Terrain terrain = point.getLocation().getTerrain();
		getTravelPoints(terrain, point.isStarport()).add(point);
	}
	
	public List<TravelPoint> getPointsForTerrain(TravelPoint nearest, Terrain to) {
		List<TravelPoint> points = new ArrayList<>();
		if (nearest.getLocation().getTerrain() == to) {
			points.addAll(getTravelPoints(to, false));
			points.addAll(getTravelPoints(to, true));
		} else if (nearest.isStarport()) {
			points.addAll(getTravelPoints(to, true));
		}
		return points;
	}
	
	public TravelPoint getNearestPoint(Location l) {
		TravelPoint nearest = null;
		double dist = Double.MAX_VALUE;
		// Checks shuttleports
		for (TravelPoint tp : getTravelPoints(l.getTerrain(), false)) {
			double tpDistance = tp.getLocation().flatDistanceTo(l);
			if (tpDistance < dist) {
				nearest = tp;
				dist = tpDistance;
			}
		}
		// Checks starports
		for (TravelPoint tp : getTravelPoints(l.getTerrain(), true)) {
			double tpDistance = tp.getLocation().flatDistanceTo(l);
			if (tpDistance < dist) {
				nearest = tp;
				dist = tpDistance;
			}
		}
		return nearest;
	}
	
	public TravelPoint getDestination(Terrain t, String destination) {
		// Check shuttleports
		for (TravelPoint tp : getTravelPoints(t, false)) {
			if (tp.getName().equals(destination))
				return tp;
		}
		// Check starports
		for (TravelPoint tp : getTravelPoints(t, true)) {
			if (tp.getName().equals(destination))
				return tp;
		}
		return null;
	}
	
	private List<TravelPoint> getTravelPoints(Terrain terrain, boolean starport) {
		Map<Terrain, List<TravelPoint>> travelPoints = starport ? starportPoints : shuttlePoints;
		List<TravelPoint> points = travelPoints.computeIfAbsent(terrain, k -> new ArrayList<>());
		return points;
	}
	
}
