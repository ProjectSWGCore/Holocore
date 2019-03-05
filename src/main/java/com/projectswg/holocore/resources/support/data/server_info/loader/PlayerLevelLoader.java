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
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public final class PlayerLevelLoader extends DataLoader {
	
	private final Map<Integer, PlayerLevelInfo> levels;
	private int maxLevel;
	
	PlayerLevelLoader() {
		this.levels = new HashMap<>();
		this.maxLevel = 0;
	}
	
	public int getMaxLevel() {
		return maxLevel;
	}
	
	@Nullable
	public PlayerLevelInfo getFromLevel(int level) {
		return levels.get(level);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/player/player_level.sdb"))) {
			levels.putAll(set.stream(PlayerLevelInfo::new).collect(toMap(PlayerLevelInfo::getLevel, Function.identity())));
			maxLevel = levels.keySet().stream().mapToInt(i -> i).max().orElse(0);
		}
	}
	
	public static class PlayerLevelInfo {
		
		private final int level;
		private final int xpRequired;
		private final String xpType;
		private final int xpMultiplier;
		private final int healthGranted;
		private final int expertisePoints;
		
		public PlayerLevelInfo(SdbResultSet set) {
			this.level = (int) set.getInt("level");
			this.xpRequired = (int) set.getInt("xp_required");
			this.xpType = set.getText("xp_type");
			this.xpMultiplier = (int) set.getInt("xp_multiplier");
			this.healthGranted = (int) set.getInt("health_granted");
			this.expertisePoints = (int) set.getInt("expertise_points");
		}
		
		public int getLevel() {
			return level;
		}
		
		public int getXpRequired() {
			return xpRequired;
		}
		
		public String getXpType() {
			return xpType;
		}
		
		public int getXpMultiplier() {
			return xpMultiplier;
		}
		
		public int getHealthGranted() {
			return healthGranted;
		}
		
		public int getExpertisePoints() {
			return expertisePoints;
		}
		
	}
}
