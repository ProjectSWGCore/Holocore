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
package com.projectswg.utility.clientdata.buildouts;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.*;

public class SnapshotLoader {
	
	private static final Set <Terrain> TERRAINS = EnumSet.of(
			Terrain.CORELLIA,	Terrain.DANTOOINE,	Terrain.DATHOMIR,
			Terrain.DUNGEON1,	Terrain.ENDOR,		Terrain.LOK,
			Terrain.NABOO,		Terrain.RORI,		Terrain.TALUS,
			Terrain.TATOOINE,	Terrain.YAVIN4
	);
	
	private final Map <Long, SWGObject> objectTable;
	private final List <SWGObject> objects;
	
	public SnapshotLoader() {
		objectTable = new HashMap<>();
		objects = new LinkedList<>();
	}
	
	public void loadAllSnapshots() {
		for (Terrain t : TERRAINS) {
			loadSnapshotsForTerrain(t);
		}
	}
	
	public void loadSnapshotsForTerrain(Terrain t) {
		TerrainSnapshotLoader loader = new TerrainSnapshotLoader(t);
		loader.load();
		objects.addAll(loader.getObjects());
		objectTable.putAll(loader.getObjectTable());
	}
	
	public Map<Long, SWGObject> getObjectTable() {
		return objectTable;
	}
	
	public List <SWGObject> getObjects() {
		return objects;
	}
	
}
