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

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class TerrainMap {
	
	private static final int CHUNK_COUNT_ACROSS = 16;
	private static final int MAP_WIDTH = 16384;
	private static final int INDEX_FACTOR = (int) (Math.log(MAP_WIDTH / (double) CHUNK_COUNT_ACROSS) / Math.log(2) + 1e-12);
	
	private final TerrainMapChunk [] chunks;
	private final ReentrantLock updateLock;
	
	public TerrainMap() {
		this.chunks = new TerrainMapChunk[CHUNK_COUNT_ACROSS*CHUNK_COUNT_ACROSS];
		this.updateLock = new ReentrantLock(false);
		for (int z = 0; z < CHUNK_COUNT_ACROSS; z++) {
			for (int x = 0; x < CHUNK_COUNT_ACROSS; x++) {
				chunks[z*CHUNK_COUNT_ACROSS+x] = new TerrainMapChunk();
			}
		}
		connectChunkNeighbors();
	}
	
	public void updateChunks() {
		if (!updateLock.tryLock())
			return;
		try {
			for (TerrainMapChunk chunk : chunks) {
				chunk.update();
			}
		} finally {
			updateLock.unlock();
		}
	}
	
	public void add(SWGObject obj) {
		move(obj);
	}
	
	public void remove(SWGObject obj) {
		TerrainMapChunk current = obj.getAwareness().setTerrainMapChunk(null);
		if (current != null) {
			current.removeObject(obj);
			obj.setAware(AwarenessType.OBJECT, List.of());
			
			for (SWGObject child : obj.getContainedObjects())
				remove(child);
			for (SWGObject child : obj.getSlottedObjects())
				remove(child);
		}
	}
	
	public void move(SWGObject obj) {
		SWGObject superParent = obj.getSuperParent();
		if (superParent != null)
			moveInParent(obj, superParent);
		else
			moveInWorld(obj);
	}
	
	private void moveInParent(SWGObject obj, SWGObject superParent) {
		TerrainMapChunk chunk = superParent.getAwareness().getTerrainMapChunk();
		TerrainMapChunk current = obj.getAwareness().setTerrainMapChunk(chunk);
		if (chunk == null)
			return; // If the parent hasn't been added to awareness yet
		
		if (current != chunk) {
			if (current != null) {
				current.removeObject(obj);
			}
			chunk.addObject(obj);
			
			for (SWGObject child : obj.getContainedObjects())
				moveInParent(child, superParent);
			for (SWGObject child : obj.getSlottedObjects())
				moveInParent(child, superParent);
		}
	}
	
	private void moveInWorld(SWGObject obj) {
		int chunkCount = CHUNK_COUNT_ACROSS;
		int indX = (obj.getTruncX()+8192) >> INDEX_FACTOR;
		int indZ = (obj.getTruncZ()+8192) >> INDEX_FACTOR;
		indX = (indX < 0) ? 0 : (indX >= chunkCount ? chunkCount-1 : indX);
		indZ = (indZ < 0) ? 0 : (indZ >= chunkCount ? chunkCount-1 : indZ);
		TerrainMapChunk chunk = chunks[indZ*CHUNK_COUNT_ACROSS+indX];
		TerrainMapChunk current = obj.getAwareness().setTerrainMapChunk(chunk);
		
		if (current != chunk) {
			if (current != null) {
				current.removeObject(obj);
			}
			chunk.addObject(obj);
			for (SWGObject child : obj.getContainedObjects())
				moveInParent(child, obj);
			for (SWGObject child : obj.getSlottedObjects())
				moveInParent(child, obj);
		}
	}
	
	private void connectChunkNeighbors() {
		for (int z = 0; z < CHUNK_COUNT_ACROSS; z++) {
			for (int x = 0; x < CHUNK_COUNT_ACROSS; x++) {
				TerrainMapChunk chunk = chunks[z*CHUNK_COUNT_ACROSS+x];
				for (int tmpZ = cappedZero(z-1); tmpZ <= z+1 && tmpZ < CHUNK_COUNT_ACROSS; tmpZ++) {
					for (int tmpX = cappedZero(x-1); tmpX <= x+1 && tmpX < CHUNK_COUNT_ACROSS; tmpX++) {
						if (x == tmpX && z == tmpZ)
							continue;
						chunk.link(chunks[tmpZ*CHUNK_COUNT_ACROSS+tmpX]);
					}
				}
			}
		}
	}
	
	private static int cappedZero(int x) {
		return x < 0 ? 0 : x;
	}
	
}
