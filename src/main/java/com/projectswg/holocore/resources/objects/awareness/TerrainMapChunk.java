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

import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

class TerrainMapChunk {
	
	private final Set<SWGObject> objects;
	
	public TerrainMapChunk() {
		this.objects = new CopyOnWriteArraySet<>();
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
	
	public List<SWGObject> getWithinAwareness(SWGObject obj) {
		List<SWGObject> withinRange = new ArrayList<>(objects.size());
		getWithinAwareness(obj, withinRange);
		return withinRange;
	}
	
	public void getWithinAwareness(SWGObject obj, Collection<SWGObject> withinRange) {
		int truncX = obj.getTruncX();
		int truncZ = obj.getTruncZ();
		int instance = obj.getInstanceLocation().getInstanceNumber();
		int loadRange = (int) obj.getLoadRange();
		objects.forEach(test -> {
			// Calculate distance
			int dTmp = truncX - test.getTruncX();
			int d = dTmp * dTmp;
			dTmp = truncZ - test.getTruncZ();
			d = (int) Math.sqrt(d + dTmp * dTmp);
			
			// Must be within load range
			if (d <= loadRange || d <= (int) test.getLoadRange()) {
				// Can't be a logged out player
				if (test.getBaselineType() != BaselineType.CREO || !((CreatureObject) test).isLoggedOutPlayer()) {
					// Can't be the same object
					if (!obj.equals(test)) {
						// Must be within the same instance number
						if (instance == test.getInstanceLocation().getInstanceNumber()) {
							withinRange.add(test);
						}
					}
				}
			}
		});
	}
	
}
