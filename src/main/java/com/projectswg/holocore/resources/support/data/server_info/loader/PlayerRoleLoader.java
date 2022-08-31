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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PlayerRoleLoader extends DataLoader {
	
	private final Map<Integer, Collection<RoleInfo>> rolesByIndex;
	private final Map<String, Collection<RoleInfo>> rolesBySkill;
	
	PlayerRoleLoader() {
		this.rolesByIndex = new HashMap<>();
		this.rolesBySkill = new HashMap<>();
	}
	
	@NotNull
	public Collection<RoleInfo> getRolesByIndex(int index) {
		return rolesByIndex.getOrDefault(index, Collections.emptyList());
	}
	
	@NotNull
	public Collection<RoleInfo> getRolesBySkill(String skill) {
		return rolesBySkill.getOrDefault(skill, Collections.emptyList());
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/player/role.sdb"))) {
			while (set.next()) {
				RoleInfo role = new RoleInfo(set);
				rolesByIndex.computeIfAbsent(role.getIndex(), index -> new HashSet<>()).add(role);
				rolesBySkill.computeIfAbsent(role.getQualifyingSkill(), index -> new HashSet<>()).add(role);
			}
		}
	}
	
	public static class RoleInfo {
		
		private final int index;
		private final String roleIcon;
		private final String qualifyingSkill;
		
		public RoleInfo(SdbResultSet set) {
			this.index = (int) set.getInt("INDEX");
			this.roleIcon = set.getText("ROLE_ICON");
			this.qualifyingSkill = set.getText("QUALIFYING_SKILL");
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
