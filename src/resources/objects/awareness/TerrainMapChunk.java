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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import resources.Location;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

class TerrainMapChunk {
	
	private final Set<SWGObject> objects;
	private final double minX;
	private final double minZ;
	private final double maxX;
	private final double maxZ;
	
	public TerrainMapChunk(double minX, double minZ, double maxX, double maxZ) {
		objects = new HashSet<>(); // There will be some expanding and shrinking
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
	}
	
	public void addObject(SWGObject obj) {
		synchronized (objects) {
			objects.add(obj);
		}
	}
	
	public void removeObject(SWGObject obj) {
		synchronized (objects) {
			objects.remove(obj);
		}
	}
	
	public boolean containsObject(SWGObject obj) {
		synchronized (objects) {
			return objects.contains(obj);
		}
	}
	
	public boolean isWithinBounds(SWGObject obj) {
		double x = obj.getX();
		double z = obj.getZ();
		return minX <= x && minZ <= z && maxX >= x && maxZ >= z;
	}
	
	public List<SWGObject> getWithinAwareness(SWGObject obj) {
		double loadRange = obj.getLoadRange();
		synchronized (objects) {
			List<SWGObject> withinRange = new LinkedList<>();
			Location objLocation = obj.getWorldLocation();
			Iterator<SWGObject> it = objects.iterator();
			while (it.hasNext()) {
				SWGObject test = it.next();
				if (test.getParent() != null) {
					it.remove();
					continue;
				}
				if (isValidWithinRange(obj, test, objLocation, loadRange))
					withinRange.add(test);
			}
			return withinRange;
		}
	}
	
	private boolean isValidWithinRange(SWGObject obj, SWGObject inRange, Location objLocation, double range) {
		if (obj.equals(inRange))
			return false;
		if (inRange instanceof CreatureObject && ((CreatureObject) inRange).isLoggedOutPlayer())
			return false;
		if (!inRange.getWorldLocation().isWithinFlatDistance(objLocation, Math.max(range, inRange.getLoadRange())))
			return false;
		return true;
	}
	
}
