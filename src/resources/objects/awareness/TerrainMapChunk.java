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
import java.util.concurrent.CopyOnWriteArraySet;

import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

class TerrainMapChunk {
	
	private final Set<SWGObject> objects;
	private final int minX;
	private final int minZ;
	private final int maxX;
	private final int maxZ;
	
	public TerrainMapChunk(int minX, int minZ, int maxX, int maxZ) {
		this.objects = new CopyOnWriteArraySet<>();
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
		int x = obj.getTruncX();
		int z = obj.getTruncZ();
		return minX <= x && minZ <= z && maxX >= x && maxZ >= z;
	}
	
	public List<SWGObject> getWithinAwareness(SWGObject obj) {
		List<SWGObject> withinRange = new ArrayList<>(objects.size() / 8);
		getWithinAwareness(obj, withinRange);
		return withinRange;
	}
	
	public void getWithinAwareness(SWGObject obj, Collection<SWGObject> withinRange) {
		int loadRange = (int) obj.getLoadRange();
		for (SWGObject test : objects) {
			if (isValidWithinRange(obj, test, loadRange))
				withinRange.add(test);
		}
	}
	
	private static boolean isValidWithinRange(SWGObject obj, SWGObject inRange, int range) {
		if (!isWithinRange(obj, inRange, range))
			return false;
		if (inRange.getBaselineType() == BaselineType.CREO && ((CreatureObject) inRange).isLoggedOutPlayer())
			return false;
		if (obj.equals(inRange))
			return false;
		return obj.getInstanceLocation().getInstanceNumber() == inRange.getInstanceLocation().getInstanceNumber();
	}
	
	private static boolean isWithinRange(SWGObject a, SWGObject b, int range) {
		return square(a.getTruncX()-b.getTruncX()) + square(a.getTruncZ()-b.getTruncZ()) <= square(Math.max((int) b.getLoadRange(), range));
	}
	
	private static int square(int x) {
		return x * x;
	}
	
}
