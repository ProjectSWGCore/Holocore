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
package resources.objects.awareness;

import com.projectswg.common.data.location.Location;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

class SpeedCheckHandler {
	
	public SpeedCheckHandler() {
		
	}
	
	public void moveObjectSpeedChecks(CreatureObject obj, Location requestedLocation) {
		double time = obj.getTimeSinceLastTransform() / 1000;
		obj.updateLastTransformTime();
		Location l = obj.getWorldLocation();
		if (isSpeeding(obj, l, requestedLocation, time)) {
			double angle = getMovementAngle(l, requestedLocation);
			requestedLocation.setX(l.getX()+obj.getMovementScale()*7.3*time*Math.cos(angle));
			requestedLocation.setZ(l.getZ()+obj.getMovementScale()*7.3*time*Math.sin(angle));
		}
	}
	
	public void moveObjectSpeedChecks(CreatureObject obj, SWGObject parent, Location requestedLocation) {
		double time = obj.getTimeSinceLastTransform() / 1000;
		obj.updateLastTransformTime();
		Location l = obj.getWorldLocation();
		Location requestedWorld = new Location(requestedLocation.getX(), 0, requestedLocation.getZ(), parent.getTerrain());
		requestedWorld.translateLocation(parent.getWorldLocation());
		if (isSpeeding(obj, l, requestedWorld, time)) {
			double angle = getMovementAngle(l, requestedWorld);
			requestedLocation.setX(requestedLocation.getX()+obj.getMovementScale()*7.3*time*invertNormalizedValue(Math.cos(angle)));
			requestedLocation.setZ(requestedLocation.getZ()+obj.getMovementScale()*7.3*time*invertNormalizedValue(Math.sin(angle)));
		}
	}
	
	private boolean isSpeeding(CreatureObject obj, Location nWorld, Location newLocation, double time) {
		return Math.sqrt(square(nWorld.getX()-nWorld.getX()) + square(nWorld.getZ()-newLocation.getZ())) / time > obj.getMovementScale()*7.3;
	}
	
	private double getMovementAngle(Location oldLocation, Location newLocation) {
		if (newLocation.getX() == oldLocation.getX())
			return Math.PI;
		return Math.atan2(newLocation.getZ()-oldLocation.getZ(), newLocation.getX()-oldLocation.getX()) + Math.PI;
	}
	
	private double invertNormalizedValue(double x) {
		if (x < 0)
			return -1 - x;
		return 1-x;
	}
	
	private double square(double x) {
		return x * x;
	}
	
}
