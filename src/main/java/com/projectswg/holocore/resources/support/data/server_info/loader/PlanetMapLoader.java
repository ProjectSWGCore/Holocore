/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;

import java.io.File;
import java.io.IOException;

public final class PlanetMapLoader extends DataLoader {
	
	PlanetMapLoader() {
		
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/player/planet_map_cat.sdb"))) {
			while (set.next()) {
				PlanetMapInfo mapInfo = new PlanetMapInfo(set);
				// TODO: Store information
			}
		}
	}
	
	public static class PlanetMapInfo {
		
		private final String name;
		private final int index;
		private final boolean category;
		private final boolean subcategory;
		private final boolean canBeActive;
		private final String faction;
		private final boolean factionVisibleOnly;
		
		public PlanetMapInfo(SdbResultSet set) {
			this.name = set.getText("name");
			this.index = (int) set.getInt("index");
			this.category = set.getBoolean("iscategory");
			this.subcategory = set.getBoolean("issubcategory");
			this.canBeActive = set.getBoolean("can_be_active");
			this.faction = set.getText("faction");
			this.factionVisibleOnly = set.getBoolean("faction_visible_only");
		}
	}
}
