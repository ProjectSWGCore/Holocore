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
			// TODO: Do something about it
		}
	}
	
	public void moveObjectSpeedChecks(CreatureObject obj, SWGObject parent, Location requestedLocation) {
		double time = obj.getTimeSinceLastTransform() / 1000;
		obj.updateLastTransformTime();
		Location l = obj.getWorldLocation();
		Location requestedWorld = Location.builder()
				.setPosition(requestedLocation.getX(), 0, requestedLocation.getZ())
				.setTerrain(parent.getTerrain())
				.translateLocation(parent.getWorldLocation())
				.build();
		if (isSpeeding(obj, l, requestedWorld, time)) {
			// TODO: Do something about it
		}
	}
	
	private static boolean isSpeeding(CreatureObject obj, Location nWorld, Location newLocation, double time) {
		return Math.sqrt(square(nWorld.getX()-nWorld.getX()) + square(nWorld.getZ()-newLocation.getZ())) / time > obj.getMovementScale()*7.3;
	}
	
	private static double square(double x) {
		return x * x;
	}
	
}
