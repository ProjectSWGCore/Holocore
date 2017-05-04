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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.projectswg.common.concurrency.SynchronizedSet;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

class TerrainMapChunk {
	
	private final Set<SWGObject> objects;
	private final double minX;
	private final double minZ;
	private final double maxX;
	private final double maxZ;
	
	public TerrainMapChunk(double minX, double minZ, double maxX, double maxZ) {
		objects = new SynchronizedSet<>(); // There will be some expanding and shrinking
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
	}
	
	public void addObject(SWGObject obj) {
		objects.add(obj);
	}
	
	public void removeObject(SWGObject obj) {
		objects.remove(obj);
	}
	
	public boolean containsObject(SWGObject obj) {
		return objects.contains(obj);
	}
	
	public boolean isWithinBounds(SWGObject obj) {
		double x = obj.getX();
		double z = obj.getZ();
		return minX <= x && minZ <= z && maxX >= x && maxZ >= z;
	}
	
	public List<SWGObject> getWithinAwareness(SWGObject obj) {
		List<SWGObject> withinRange = new ArrayList<>(objects.size() / 8);
		getWithinAwareness(obj, withinRange);
		return withinRange;
	}
	
	public void getWithinAwareness(SWGObject obj, Collection<SWGObject> withinRange) {
		double loadRange = obj.getLoadRange();
		synchronized (objects) {
			for (SWGObject test : objects) {
				if (isValidWithinRange(obj, test, loadRange))
					withinRange.add(test);
			}
		}
	}
	
	private boolean isValidWithinRange(SWGObject obj, SWGObject inRange, double range) {
		if (obj.equals(inRange))
			return false;
		if (inRange instanceof CreatureObject && ((CreatureObject) inRange).isLoggedOutPlayer())
			return false;
		if (!isWithinRange(obj, inRange, range))
			return false;
		return true;
	}
	
	private boolean isWithinRange(SWGObject a, SWGObject b, double range) {
		return square(a.getX()-b.getX()) + square(a.getZ()-b.getZ()) <= square(Math.max(b.getLoadRange(), range));
	}
	
	private double square(double x) {
		return x * x;
	}
	
}
