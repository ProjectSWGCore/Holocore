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
package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class BuildingLoader extends DataLoader {
	
	private final Map<String, BuildingLoaderInfo> buildingMap;
	
	BuildingLoader() {
		this.buildingMap = new HashMap<>();
	}
	
	public BuildingLoaderInfo getBuilding(String buildingName) {
		return buildingMap.get(buildingName);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/building/buildings.sdb"))) {
			while (set.next()) {
				buildingMap.put(set.getText(0), new BuildingLoaderInfo(set));
			}
		}
	}
	
	public static class BuildingLoaderInfo {
		
		private final String name;
		private final long id;
		private final Terrain terrain;
		
		public BuildingLoaderInfo(SdbResultSet set) {
			this.name = set.getText(0).intern();
			this.id = set.getInt(2);
			this.terrain = Terrain.valueOf(set.getText(1));
		}
		
		public String getName() {
			return name;
		}
		
		public long getId() {
			return id;
		}
		
		public Terrain getTerrain() {
			return terrain;
		}
		
	}
	
}
