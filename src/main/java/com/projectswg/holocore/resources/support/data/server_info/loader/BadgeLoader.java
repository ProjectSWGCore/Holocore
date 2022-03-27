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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class BadgeLoader extends DataLoader {
	
	private final Map<String, BadgeInfo> badgeFromKey;
	
	BadgeLoader() {
		this.badgeFromKey = new HashMap<>();
	}
	
	@Nullable
	public BadgeLoader.BadgeInfo getBadgeFromKey(String name) {
		return badgeFromKey.get(name);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/badges/badge_map.sdb"))) {
			while (set.next()) {
				BadgeInfo badgeInfo = new BadgeInfo(set);
				badgeFromKey.put(badgeInfo.getKey(), badgeInfo);
			}
		}
	}
	
	public static class BadgeInfo {
		
		private final int index;
		private final String key;
		private final String music;
		private final int category;	// TODO enum?
		private final int show;	// TODO enum?
		private final String type;	// TODO enum?
		private final boolean title;
		
		public BadgeInfo(SdbResultSet set) {
			index = (int) set.getInt("INDEX");
			key = set.getText("KEY");
			music = set.getText("MUSIC");
			category = (int) set.getInt("CATEGORY");
			show = (int) set.getInt("SHOW");
			type = set.getText("TYPE");
			title = set.getBoolean("IS_TITLE");
		}
		
		public int getIndex() {
			return index;
		}
		
		public String getKey() {
			return key;
		}
		
		public String getMusic() {
			return music;
		}
		
		public int getCategory() {
			return category;
		}
		
		public int getShow() {
			return show;
		}
		
		public String getType() {
			return type;
		}
		
		public boolean isTitle() {
			return title;
		}
	}
}
