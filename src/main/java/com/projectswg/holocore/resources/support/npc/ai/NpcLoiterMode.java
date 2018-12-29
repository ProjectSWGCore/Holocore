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
import com.projectswg.common.data.location.Location.LocationBuilder;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI object that loiters the area
 */
public class NpcLoiterMode extends NpcMode {
	
	private Location mainLocation;
	private double radius;
	
	public NpcLoiterMode(AIObject obj, double radius) {
		super(obj);
		this.mainLocation = null;
		this.radius = radius;
	}
	
	@Override
	public void onModeStart() {
		Location currentLocation = getAI().getLocation();
		if (mainLocation == null)
			mainLocation = currentLocation;
		if (mainLocation.distanceTo(currentLocation) >= 1)
			runTo(mainLocation);
	}
	
	@Override
	public void act() {
		Random random = ThreadLocalRandom.current();
		if (isRooted()) {
			queueNextLoop(10000 + random.nextInt(5));
			return;
		}
		Location currentLocation = mainLocation;
		
		if (random.nextDouble() > 0.25) // Only a 25% movement chance
			return;
		double dist = Math.sqrt(radius);
		double theta;
		LocationBuilder l = Location.builder(currentLocation);
		do {
			theta = random.nextDouble() * Math.PI * 2;
			l.setX(currentLocation.getX() + Math.cos(theta) * dist);
			l.setZ(currentLocation.getZ() + Math.sin(theta) * dist);
		} while (!l.isWithinFlatDistance(mainLocation, radius));
		l.setHeading(l.getYaw() - Math.toDegrees(theta));
		
		moveTo(l.build());
		queueNextLoop(30 + random.nextInt(10));
	}
	
}
