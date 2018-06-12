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
package com.projectswg.holocore.resources.support.objects.swg.custom;

import com.projectswg.common.data.location.Location;

import java.util.concurrent.TimeUnit;

/**
 * Boring AI object that just sits in the same location.  aiLoop() can be extended for other AI objects that want random movements
 */
public class RandomAIObject extends AIObject {
	
	private Location mainLocation;
	
	public RandomAIObject(long objectId) {
		super(objectId);
	}
	
	public Location getMainLocation() {
		return mainLocation;
	}
	
	public void setMainLocation(Location mainLocation) {
		this.mainLocation = mainLocation;
	}
	
	@Override
	protected void aiInitialize() {
		long delay = (long) (30E3 + Math.random() * 10E3);
		setSchedulerProperties(delay, delay, TimeUnit.MILLISECONDS); // Using milliseconds allows for more distribution between AI loops
	}
	
	@Override
	public void aiStart() {
		super.aiStart();
		if (mainLocation == null) {
			// If no location is given, then use object location
			setMainLocation(getLocation());
		}
	}
	
	@Override
	protected void aiLoop() {
		
	}
	
}
