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

public final class PlayerRoleLoader extends DataLoader {
	
	private final Map<Integer, RoleInfo> rolesByIndex;
	private final Map<String, RoleInfo> rolesBySkill;
	
	PlayerRoleLoader() {
		this.rolesByIndex = new HashMap<>();
		this.rolesBySkill = new HashMap<>();
	}
	
	@Nullable
	public RoleInfo getRoleByIndex(int index) {
		return rolesByIndex.get(index);
	}
	
	@Nullable
	public RoleInfo getRoleBySkill(String skill) {
		return rolesBySkill.get(skill);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/player/role.sdb"))) {
			while (set.next()) {
				RoleInfo role = new RoleInfo(set);
				rolesByIndex.put(role.getIndex(), role);
				rolesBySkill.put(role.getQualifyingSkill(), role);
			}
		}
	}
	
	public static class RoleInfo {
		
		private final int index;
		private final String roleIcon;
		private final String qualifyingSkill;
		
		public RoleInfo(SdbResultSet set) {
			this.index = (int) set.getInt("index");
			this.roleIcon = set.getText("role_icon");
			this.qualifyingSkill = set.getText("qualifying_skill");
		}
		
		public int getIndex() {
			return index;
		}
		
		public String getRoleIcon() {
			return roleIcon;
		}
		
		public String getQualifyingSkill() {
			return qualifyingSkill;
		}
		
	}
}
