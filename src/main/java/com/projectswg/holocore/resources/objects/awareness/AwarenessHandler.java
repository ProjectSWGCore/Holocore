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

import java.util.EnumMap;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.awareness.TerrainMap.TerrainMapCallback;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.creature.CreatureState;

public class AwarenessHandler implements AutoCloseable {
	
	private final EnumMap<Terrain, TerrainMap> terrains;
	
	public AwarenessHandler(TerrainMapCallback callback) {
		terrains = new EnumMap<>(Terrain.class);
		loadTerrainMaps(callback);
	}
	
	@Override
	public void close() {
		for (TerrainMap map : terrains.values()) {
			map.stop();
		}
	}
	
	private void loadTerrainMaps(TerrainMapCallback callback) {
		for (Terrain t : Terrain.values()) {
			TerrainMap map = new TerrainMap(t, callback);
			map.start();
			terrains.put(t, map);
		}
	}
	
	public void moveObject(SWGObject obj, Location requestedLocation) {
		// Update location
		updateLocation(obj, null, requestedLocation);
		// Update awareness
		Terrain terrain = requestedLocation.getTerrain();
		if (terrain != null && terrain != Terrain.GONE) {
			TerrainMap map = getTerrainMap(terrain);
			map.moveWithinMap(obj);
		}
	}
	
	public void moveObject(SWGObject obj, SWGObject parent, Location requestedLocation) {
		Assert.notNull(parent);
		// Remove from previous awareness
		TerrainMap.removeWithoutUpdate(obj);
		// Update location
		updateLocation(obj, parent, requestedLocation);
		// Update awareness
		obj.resetAwareness();
	}

	public void disappearObject(SWGObject obj, boolean disappearObjects, boolean disappearCustom) {
		if (disappearObjects) {
			Terrain terrain = obj.getTerrain();
			if (terrain != null && terrain != Terrain.GONE) {
				TerrainMap map = getTerrainMap(terrain);
				map.removeFromMap(obj);
			}
		} else {
			TerrainMap.removeWithoutUpdate(obj);
		}
		if (disappearCustom)
			obj.clearCustomAware(true);
	}
	
	private TerrainMap getTerrainMap(Terrain t) {
		return terrains.get(t);
	}
	
	private static void updateLocation(SWGObject obj, SWGObject parent, Location requestedLocation) {
		obj.setLocation(requestedLocation);
		if (isRider(obj, parent))
			obj.moveToContainer(parent);
		obj.onObjectMoved();
	}
	
	private static boolean isRider(SWGObject obj, SWGObject parent) {
		return obj.getParent() != parent && !(obj.getBaselineType() == BaselineType.CREO && ((CreatureObject) obj).isStatesBitmask(CreatureState.RIDING_MOUNT));
	}
	
}
