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
import java.util.HashSet;
import java.util.Set;

public class TerrainMap {
	
	private static final int CHUNK_COUNT_ACROSS = 16;
	private static final int MAP_WIDTH = 16384;
	private static final int INDEX_FACTOR = (int) (Math.log(MAP_WIDTH / CHUNK_COUNT_ACROSS) / Math.log(2) + 1e-12);
	
	private final TerrainMapChunk [][] chunks;
	
	public TerrainMap() {
		this.chunks = new TerrainMapChunk[CHUNK_COUNT_ACROSS][CHUNK_COUNT_ACROSS];
		for (int z = 0; z < CHUNK_COUNT_ACROSS; z++) {
			for (int x = 0; x < CHUNK_COUNT_ACROSS; x++) {
				chunks[z][x] = new TerrainMapChunk();
			}
		}
	}
	
	public void add(SWGObject obj) {
		if (obj.getAwareness().getTerrainMapChunk() == null)
			move(obj);
	}
	
	public void remove(SWGObject obj) {
		TerrainMapChunk current = obj.getAwareness().setTerrainMapChunk(null);
		if (current != null)
			current.removeObject(obj);
	}
	
	public void update(SWGObject obj) {
		obj.setAware(AwarenessType.OBJECT, getAware(obj));
		obj.onObjectMoved();
	}
	
	public void move(SWGObject obj) {
		TerrainMapChunk chunk = chunks[calculateIndex(obj.getTruncZ())][calculateIndex(obj.getTruncX())];
		TerrainMapChunk current = obj.getAwareness().setTerrainMapChunk(chunk);
		if (current != chunk) {
			if (current != null)
				current.removeObject(obj);
			chunk.addObject(obj);
		}
	}
	
	@NotNull
	private Collection<SWGObject> getAware(SWGObject obj) {
		SWGObject superParent = obj.getSuperParent();
		Set<SWGObject> aware;
		if (!AwarenessUtilities.isInAwareness(obj))
			aware = new HashSet<>();
		else if (superParent == null)
			aware = getNearbyAware(obj);
		else
			aware = superParent.getAware(AwarenessType.OBJECT);
		aware.removeIf(a -> !AwarenessUtilities.isInAwareness(a));
		recursiveAdd(aware, obj);
		return aware;
	}
	
	@NotNull
	private Set<SWGObject> getNearbyAware(SWGObject obj) {
		if (obj.getAwareness().getTerrainMapChunk() == null || !AwarenessUtilities.isInAwareness(obj))
			return new HashSet<>();
		
		int countAcross = CHUNK_COUNT_ACROSS;
		int sX = ((obj.getTruncX()+8192) >> INDEX_FACTOR) - 1;
		int sZ = ((obj.getTruncZ()+8192) >> INDEX_FACTOR) - 1;
		int eX = sX + 2;
		int eZ = sZ + 2;
		if (sX < 0)
			sX = 0;
		if (sZ < 0)
			sZ = 0;
		if (eX < 0)
			eX = 0;
		if (eZ < 0)
			eZ = 0;
		if (sX >= countAcross)
			sX = countAcross-1;
		if (sZ >= countAcross)
			sZ = countAcross-1;
		if (eX >= countAcross)
			eX = countAcross-1;
		if (eZ >= countAcross)
			eZ = countAcross-1;
		
		Set<SWGObject> aware = new HashSet<>();
		for (int z = sZ; z <= eZ; ++z) {
			for (int x = sX; x <= eX; ++x) {
				chunks[z][x].getWithinAwareness(obj, aware);
			}
		}
		return aware;
	}
	
	private static void recursiveAdd(@NotNull Collection<SWGObject> aware, @NotNull SWGObject obj) {
		aware.add(obj);
		for (SWGObject child : obj.getSlottedObjects()) {
			recursiveAdd(aware, child);
		}
		for (SWGObject child : obj.getContainedObjects()) {
			recursiveAdd(aware, child);
		}
	}
	
	private static int calculateIndex(int x) {
		int i = (x+8192) >> INDEX_FACTOR;
		if (i < 0)
			return 0;
		if (i >= CHUNK_COUNT_ACROSS)
			return CHUNK_COUNT_ACROSS-1;
		return i;
	}
	
}
