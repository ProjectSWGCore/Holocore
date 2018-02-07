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

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

import javax.annotation.Nonnull;
import java.util.*;

public class TerrainMap {
	
	private static final int CHUNK_COUNT_ACROSS = 16;
	private static final int MAP_WIDTH = 16384;
	private static final int INDEX_FACTOR = (int) (Math.log(MAP_WIDTH / CHUNK_COUNT_ACROSS) / Math.log(2) + 1e-12);
	
	private final TerrainMapCallback callback;
	private final TerrainMapChunk [][] chunks;
	
	public TerrainMap(Terrain t, TerrainMapCallback callback) {
		this.callback = callback;
		this.chunks = new TerrainMapChunk[CHUNK_COUNT_ACROSS][CHUNK_COUNT_ACROSS];
		for (int z = 0; z < CHUNK_COUNT_ACROSS; z++) {
			for (int x = 0; x < CHUNK_COUNT_ACROSS; x++) {
				chunks[z][x] = new TerrainMapChunk();
			}
		}
	}
	
	public void start() {
		
	}
	
	public void stop() {
		
	}
	
	public void moveWithinMap(SWGObject obj) {
		if (isInAwareness(obj)) {
			move(obj);
			update(obj);
			callback.onMoveSuccess(obj);
		} else {
			callback.onMoveFailure(obj);
		}
	}
	
	public void removeFromMap(SWGObject obj) {
		if (removeWithoutUpdate(obj)) {
			update(obj);
			Assert.test(isInAwareness(obj));
		}
	}
	
	private void move(SWGObject obj) {
		TerrainMapChunk chunk = getChunk(obj);
		TerrainMapChunk current = obj.getAwareness().setTerrainMapChunk(chunk);
		if (current == chunk)
			return; // Ignore if it doesn't change
		if (current != null)
			current.removeObject(obj);
		chunk.addObject(obj);
	}
	
	private void update(SWGObject obj) {
		final Collection<SWGObject> oldAware = obj.getObjectsAware();
		final Collection<SWGObject> newAware = getNearbyAware(obj);
		final TerrainMapCallback call = this.callback;
		newAware.forEach(n -> {
			if (!oldAware.contains(n))
				call.onWithinRange(obj, n);
		});
		oldAware.forEach(p -> {
			if (!newAware.contains(p))
				call.onOutOfRange(obj, p);
		});
	}
	
	private Set<SWGObject> getNearbyAware(SWGObject obj) {
		if (obj.getAwareness().getTerrainMapChunk() == null)
			return new HashSet<>();
		
		int sX = calculateIndex(obj.getTruncX())-1;
		int sZ = calculateIndex(obj.getTruncZ())-1;
		int eX = sX + 2;
		int eZ = sZ + 2;
		if (sX < 0)
			sX = 0;
		if (sZ < 0)
			sZ = 0;
		if (eX >= CHUNK_COUNT_ACROSS)
			eX = CHUNK_COUNT_ACROSS-1;
		if (eZ >= CHUNK_COUNT_ACROSS)
			eZ = CHUNK_COUNT_ACROSS-1;
		
		List<SWGObject> aware = new ArrayList<>(128);
		for (int z = sZ; z <= eZ; ++z) {
			for (int x = sX; x <= eX; ++x) {
				chunks[z][x].getWithinAwareness(obj, aware);
			}
		}
		return new HashSet<>(aware);
	}
	
	@Nonnull
	private TerrainMapChunk getChunk(SWGObject obj) {
		return chunks[calculateIndex(obj.getTruncZ())][calculateIndex(obj.getTruncX())];
	}
	
	public static boolean removeWithoutUpdate(SWGObject obj) {
		TerrainMapChunk chunk = obj.getAwareness().setTerrainMapChunk(null);
		if (chunk != null)
			chunk.removeObject(obj);
		return chunk != null;
	}
	
	private static boolean isInAwareness(SWGObject obj) {
		return obj.getParent() == null && obj.getBaselineType() != BaselineType.WAYP
				&& (obj.getBaselineType() != BaselineType.CREO || !((CreatureObject) obj).isPlayer() || ((CreatureObject) obj).isLoggedInPlayer());
	}
	
	private static int calculateIndex(int x) {
		int i = (x+8192) >> INDEX_FACTOR;
		if (i < 0)
			return 0;
		if (i >= CHUNK_COUNT_ACROSS)
			return CHUNK_COUNT_ACROSS-1;
		return i;
	}
	
	public interface TerrainMapCallback {
		void onWithinRange(SWGObject obj, SWGObject inRange);
		void onOutOfRange(SWGObject obj, SWGObject outRange);
		void onMoveSuccess(SWGObject obj);
		void onMoveFailure(SWGObject obj);
	}
	
}
