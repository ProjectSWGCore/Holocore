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
package com.projectswg.holocore.resources.support.objects.awareness;

import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class TerrainMapChunk {
	
	private final List<SWGObject> objects;
	private TerrainMapChunk [] neighbors;
	
	public TerrainMapChunk() {
		this.objects = new CopyOnWriteArrayList<>();
		this.neighbors = new TerrainMapChunk[0];
	}
	
	public void link(TerrainMapChunk neighbor) {
		int length = neighbors.length;
		neighbors = Arrays.copyOf(neighbors, length+1);
		neighbors[length] = neighbor;
	}
	
	public void addObject(@NotNull SWGObject obj) {
		objects.add(obj);
	}
	
	public void removeObject(@NotNull SWGObject obj) {
		objects.remove(obj);
	}
	
	public Set<SWGObject> getWithinAwareness(@NotNull SWGObject obj) {
		Set<SWGObject> withinRange = new HashSet<>();
		getWithinAwareness(obj, withinRange);
		for (TerrainMapChunk neighbor : neighbors) {
			neighbor.getWithinAwareness(obj, withinRange);
		}
		return withinRange;
	}
	
	public void getWithinAwareness(@NotNull SWGObject obj, @NotNull Collection<SWGObject> withinRange) {
		for (SWGObject test : objects) {
			if (obj.isWithinAwarenessRange(test))
				recursiveAdd(withinRange, obj, test);
		}
	}
	
	private static void recursiveAdd(@NotNull Collection<SWGObject> withinRange, @NotNull SWGObject obj, @NotNull SWGObject test) {
		if (!obj.isWithinAwarenessRange(test))
			return;
		withinRange.add(test);
		for (SWGObject child : test.getSlottedObjects()) {
			recursiveAdd(withinRange, obj, child);
		}
		for (SWGObject child : test.getContainedObjects()) {
			recursiveAdd(withinRange, obj, child);
		}
	}
	
}
