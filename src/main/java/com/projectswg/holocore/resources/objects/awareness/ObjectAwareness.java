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

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.creature.CreatureState;

import javax.annotation.Nonnull;
import java.util.Collections;

public class ObjectAwareness {
	
	private final TerrainMap[] terrains;
	
	public ObjectAwareness() {
		terrains = new TerrainMap[Terrain.values().length];
		for (int i = 0; i < terrains.length; i++) {
			terrains[i] = new TerrainMap();
		}
	}
	
	/**
	 * Called when an object was created
	 *
	 * @param obj the object created
	 */
	public void createObject(@Nonnull SWGObject obj) {
		if (AwarenessUtilities.isInAwareness(obj) && obj.getParent() == null) {
			TerrainMap map = getTerrainMap(obj);
			map.add(obj);
			map.update(obj);
		}
	}
	
	/**
	 * Called when an object is destroyed
	 *
	 * @param obj the object destroyed
	 */
	public void destroyObject(@Nonnull SWGObject obj) {
		TerrainMap map = getTerrainMap(obj);
		map.remove(obj);
		map.update(obj);
	}
	
	/**
	 * Called when an object needs an update
	 *
	 * @param obj the object to update
	 */
	public void updateObject(@Nonnull SWGObject obj) {
		SWGObject superParent = obj.getSuperParent();
		TerrainMap map = getTerrainMap(obj);
		if (superParent != null) {
			assert getTerrainMap(superParent) == map : "super parent terrain must match child terrain";
			map.remove(obj);
			map.update(superParent);
		} else {
			map.move(obj);
		}
		map.update(obj);
	}
	
	@Nonnull
	private TerrainMap getTerrainMap(SWGObject obj) {
		return terrains[obj.getTerrain().ordinal()];
	}
	
	private static boolean isRider(@Nonnull SWGObject obj, SWGObject parent) {
		return obj.getParent() != parent && !(obj.getBaselineType() == BaselineType.CREO && ((CreatureObject) obj).isStatesBitmask(CreatureState.RIDING_MOUNT));
	}
	
}
