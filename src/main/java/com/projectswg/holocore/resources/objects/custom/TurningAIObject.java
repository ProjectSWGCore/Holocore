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

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI object that loiters the area
 */
public class TurningAIObject extends RandomAIObject {
	
	private final AtomicInteger updateCounter;
	
	public TurningAIObject(long objectId) {
		super(objectId);
		this.updateCounter = new AtomicInteger(0);
	}
	
	@Override
	protected void aiLoop() {
		if (isInCombat() || !canAiMove() || !hasNearbyPlayers())
			return;
		Random r = new Random();
		if (r.nextDouble() > 0.25) // Only a 25% movement chance
			return;
		if (getObservers().isEmpty()) // No need to dance if nobody is watching
			return;
		double theta = r.nextDouble() * 360;
		new MoveObjectIntent(this, getParent(), Location.builder(getMainLocation()).setHeading(theta).build(), 1.37, updateCounter.getAndIncrement()).broadcast();
	}
	
}
