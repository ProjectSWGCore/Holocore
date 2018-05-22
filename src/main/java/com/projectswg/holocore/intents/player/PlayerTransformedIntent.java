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
package com.projectswg.holocore.intents.player;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;

public class PlayerTransformedIntent extends Intent {
	
	private final CreatureObject object;
	private final SWGObject oldParent;
	private final SWGObject newParent;
	private final Location oldLocation;
	private final Location newLocation;
	
	public PlayerTransformedIntent(CreatureObject object, SWGObject oldParent, SWGObject newParent, Location oldLocation, Location newLocation) {
		this.object = object;
		this.oldParent = oldParent;
		this.newParent = newParent;
		this.oldLocation = oldLocation;
		this.newLocation = newLocation;
	}
	
	public CreatureObject getPlayer() {
		return object;
	}
	
	public SWGObject getOldParent() {
		return oldParent;
	}
	
	public SWGObject getNewParent() {
		return newParent;
	}
	
	public Location getOldLocation() {
		return oldLocation;
	}
	
	public Location getNewLocation() {
		return newLocation;
	}
	
	public boolean changedParents() {
		return oldParent != newParent;
	}
	
	public boolean enteredParentFromWorld() {
		return oldParent == null && newParent != null;
	}
	
	public boolean enteredArea(Location l, double radius) {
		return enteredArea(l.getTerrain(), l.getX(), l.getY(), l.getZ(), radius);
	}
	
	public boolean enteredArea(Terrain t, double x, double y, double z, double radius) {
		return newLocation.isWithinDistance(t, x, y, z, radius) && !oldLocation.isWithinDistance(t, x, y, z, radius);
	}
	
}
