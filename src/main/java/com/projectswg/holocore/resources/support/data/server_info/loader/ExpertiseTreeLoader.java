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

public final class ExpertiseTreeLoader extends DataLoader {
	
	private final Map<Integer, ExpertiseTreeInfo> trees;
	
	ExpertiseTreeLoader() {
		this.trees = new HashMap<>();
	}
	
	@Nullable
	public ExpertiseTreeInfo getTreeFromId(int id) {
		return trees.get(id);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/expertise/expertise_trees.sdb"))) {
			while (set.next()) {
				ExpertiseTreeInfo treeInfo = new ExpertiseTreeInfo(set);
				trees.put(treeInfo.getId(), treeInfo);
			}
		}
	}
	
	public static class ExpertiseTreeInfo {
		
		private final int id;
		private final String stringId;
		private final String uiBackgroundId;
		
		public ExpertiseTreeInfo(SdbResultSet set) {
			this.id = (int) set.getInt("expertise_tree_id");
			this.stringId = set.getText("expertise_tree_string_id");
			this.uiBackgroundId = set.getText("ui_background_id");
		}
		
		public int getId() {
			return id;
		}
		
		public String getStringId() {
			return stringId;
		}
		
		public String getUiBackgroundId() {
			return uiBackgroundId;
		}
		
	}
}
