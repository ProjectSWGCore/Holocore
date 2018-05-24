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
package com.projectswg.holocore.resources.objects.awareness;

import com.projectswg.holocore.resources.objects.SWGObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class TerrainMapChunk {
	
	private final List<SWGObject> objects;
	
	public TerrainMapChunk() {
		this.objects = new CopyOnWriteArrayList<>();
	}
	
	public void addObject(@NotNull SWGObject obj) {
		assert !objects.contains(obj) : "the chunk already contains this object";
		objects.add(obj);
	}
	
	public void removeObject(@NotNull SWGObject obj) {
		objects.remove(obj);
	}
	
	public void getWithinAwareness(@NotNull SWGObject obj, @NotNull Collection<SWGObject> withinRange) {
		int truncX = obj.getTruncX();
		int truncZ = obj.getTruncZ();
		int instance = obj.getInstanceLocation().getInstanceNumber();
		int loadRange = obj.getLoadRange();
		for (SWGObject test : objects) {
			// Calculate distance
			int dTmp = truncX - test.getTruncX();
			int d = dTmp * dTmp;
			dTmp = truncZ - test.getTruncZ();
			
			int range = test.getLoadRange();
			if (range < loadRange)
				range = loadRange;
			range = range * range;
			
			// Must be within load range and the same instance
			if ((d + dTmp * dTmp) < range && instance == test.getInstanceLocation().getInstanceNumber()) {
				recursiveAdd(withinRange, obj, test);
			}
		}
	}
	
	private static void recursiveAdd(@NotNull Collection<SWGObject> withinRange, @NotNull SWGObject obj, @NotNull SWGObject test) {
		if (!test.isVisible(obj))
			return;
		withinRange.add(test);
		for (SWGObject child : test.getSlots().values()) {
			if (child != null)
				recursiveAdd(withinRange, obj, child);
		}
		for (SWGObject child : test.getContainedObjects()) {
			recursiveAdd(withinRange, obj, child);
		}
	}
	
}
