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
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class TerrainMapChunk {
	
	private static final List<SWGObject> EMPTY_AWARENESS = List.of();
	
	private final CopyOnWriteArrayList<SWGObject> objects;
	private final CopyOnWriteArrayList<CreatureObject> creatures;
	private TerrainMapChunk [] neighbors;
	
	public TerrainMapChunk() {
		this.objects = new CopyOnWriteArrayList<>();
		this.creatures = new CopyOnWriteArrayList<>();
		this.neighbors = new TerrainMapChunk[]{this};
	}
	
	public void link(TerrainMapChunk neighbor) {
		assert this != neighbor;
		int length = neighbors.length;
		neighbors = Arrays.copyOf(neighbors, length+1);
		neighbors[length] = neighbor;
	}
	
	public void addObject(@NotNull SWGObject obj) {
		for (TerrainMapChunk neighbor : neighbors)
			neighbor.objects.addIfAbsent(obj);
		
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isPlayer())
			creatures.add((CreatureObject) obj);
	}
	
	public void removeObject(@NotNull SWGObject obj) {
		for (TerrainMapChunk neighbor : neighbors)
			neighbor.objects.remove(obj);
		
		if (obj instanceof CreatureObject)
			creatures.remove(obj);
	}
	
	public void update() {
		if (creatures.isEmpty())
			return;
		List<CreatureAware> aware = new ArrayList<>(creatures.size());
		for (CreatureObject creature : creatures) {
			if (creature.isLoggedInPlayer())
				aware.add(new CreatureAware(creature));
			else
				creature.setAware(AwarenessType.OBJECT, EMPTY_AWARENESS);
		}
		
		final CreatureAware [] awareCompiled = aware.toArray(new CreatureAware[0]);
		for (SWGObject test : objects) {
			for (CreatureAware creatureAware : awareCompiled)
				creatureAware.test(test);
		}
		
		aware.forEach(CreatureAware::commit);
	}
	
	private static class CreatureAware {
		
		private final CreatureObject creature;
		private final List<SWGObject> aware;
		
		public CreatureAware(CreatureObject creature) {
			this.creature = creature;
			this.aware = new ArrayList<>();
		}
		
		public void test(SWGObject test) {
			if (creature.isWithinAwarenessRange(test))
				aware.add(test);
		}
		
		public void commit() {
			creature.setAware(AwarenessType.OBJECT, aware);
			creature.flushAwareness();
		}
		
	}
	
}
