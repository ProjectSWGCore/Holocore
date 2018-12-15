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

import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

class TerrainMapChunk {
	
	private final Set<SWGObject> objects;
	private TerrainMapChunk [] neighbors;
	
	public TerrainMapChunk() {
		this.objects = new CopyOnWriteArraySet<>();
		this.neighbors = new TerrainMapChunk[]{this};
	}
	
	public void link(TerrainMapChunk neighbor) {
		assert this != neighbor;
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
	
	public void scan(Consumer<SWGObject> consumer) {
		for (SWGObject test : objects) {
			if (test.getBaselineType() == BaselineType.CREO)
				consumer.accept(test);
		}
	}
	
	public Collection<SWGObject> getWithinAwareness(@NotNull CreatureObject obj) {
		Set<SWGObject> withinRange = new HashSet<>();
		
		for (TerrainMapChunk neighbor : neighbors) {
			for (SWGObject test : neighbor.objects) {
				if (obj.isWithinAwarenessRange(test))
					withinRange.add(test);
			}
		}
		
		return withinRange;
	}
	
}
