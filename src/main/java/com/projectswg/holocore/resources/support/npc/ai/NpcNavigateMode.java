/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import org.jetbrains.annotations.NotNull;

public class NpcNavigateMode extends NpcMode {

	private final NavigationPoint destination;
	private final Location destinationWorldLocation;

	public NpcNavigateMode(@NotNull AIObject obj, @NotNull NavigationPoint destination) {
		super(obj);
		this.destination = destination;
		this.destinationWorldLocation = (destination.getParent() == null) ? destination.getLocation() : Location.builder(destination.getLocation()).translateLocation(destination.getParent().getWorldLocation()).build();
	}

	@Override
	public void onModeStart() {
		runTo(destination.getParent(), destination.getLocation());
	}

	@Override
	public void act() {
		Location cur = getAI().getWorldLocation();
		if (cur.distanceTo(destinationWorldLocation) < 1E-3) {
			new ScheduleNpcModeIntent(getAI(), null).broadcast();
		} else {
			queueNextLoop(500);
		}
	}
}
