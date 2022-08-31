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
import java.util.Collection;
import java.util.LinkedList;

public final class PlayerLevelLoader extends DataLoader {
	
	private final Collection<PlayerLevelInfo> levels;
	
	PlayerLevelLoader() {
		this.levels = new LinkedList<>();
	}
	
	public Collection<PlayerLevelInfo> getPlayerLevelInfos() {
		return levels;
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/experience/player_level.sdb"))) {
			while (set.next()) {
				PlayerLevelInfo playerLevelInfo = new PlayerLevelInfo(set);
				levels.add(playerLevelInfo);
			}
		}
	}
	
	public static class PlayerLevelInfo {
		
		private final int level;
		private final int requiredCombatXp;
		private final int healthAdded;
		
		public PlayerLevelInfo(SdbResultSet set) {
			this.level = (int) set.getInt("level");
			this.requiredCombatXp = (int) set.getInt("required_combat_xp");
			this.healthAdded = (int) set.getInt("level_health_added");
		}
		
		public int getLevel() {
			return level;
		}
		
		public int getRequiredCombatXp() {
			return requiredCombatXp;
		}
		
		public int getHealthAdded() {
			return healthAdded;
		}
		
	}
}
