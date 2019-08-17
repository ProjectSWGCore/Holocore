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
import com.projectswg.holocore.resources.support.data.server_info.loader.ExpertiseTreeLoader.ExpertiseTreeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ExpertiseLoader extends DataLoader {
	
	private final Map<String, ExpertiseInfo> expertiseByName;
	private final Map<ExpertiseTreeInfo, List<ExpertiseInfo>> expertiseByTree;
	
	ExpertiseLoader() {
		this.expertiseByName = new HashMap<>();
		this.expertiseByTree = new HashMap<>();
	}
	
	@Nullable
	public ExpertiseInfo getByName(String name) {
		return expertiseByName.get(name);
	}
	
	@NotNull
	public Collection<ExpertiseInfo> getPeerExpertise(@NotNull ExpertiseInfo expertise) {
		Collection<ExpertiseInfo> ret = expertiseByTree.get(expertise.getTree());
		return ret == null ? List.of() : Collections.unmodifiableCollection(ret);
	}
	
	@NotNull
	public Collection<ExpertiseInfo> getExpertiseByTree(ExpertiseTreeInfo tree) {
		Collection<ExpertiseInfo> ret = expertiseByTree.get(tree);
		return ret == null ? List.of() : Collections.unmodifiableCollection(ret);
	}
	
	@NotNull
	public Collection<ExpertiseInfo> getAllExpertise() {
		return Collections.unmodifiableCollection(expertiseByName.values());
	}
	
	@Override
	public final void load() throws IOException {
		ExpertiseTreeLoader trees = DataLoader.Companion.expertiseTrees();
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/expertise/expertise.sdb"))) {
			while (set.next()) {
				ExpertiseInfo expertise = new ExpertiseInfo(set, trees);
				expertiseByName.put(expertise.getName(), expertise);
				expertiseByTree.computeIfAbsent(expertise.getTree(), t -> new ArrayList<>()).add(expertise);
			}
		}
	}
	
	public static class ExpertiseInfo {
		
		private final String name;
		private final ExpertiseTreeInfo tree;
		private final int tier;
		private final int grid;
		private final int rank;
		private final int requiredLevel;
		private final String requiredFaction;
		private final String requiredProfession;
		
		public ExpertiseInfo(SdbResultSet set, ExpertiseTreeLoader trees) {
			this.name = set.getText("name");
			this.tree = trees.getTreeFromId((int) set.getInt("tree"));
			this.tier = (int) set.getInt("tier");
			this.grid = (int) set.getInt("grid");
			this.rank = (int) set.getInt("rank");
			this.requiredLevel = (int) set.getInt("prereq_level");
			this.requiredFaction = set.getText("prereq_faction");
			this.requiredProfession = formatProfession(set.getText("req_prof"));
		}
		
		public String getName() {
			return name;
		}
		
		public ExpertiseTreeInfo getTree() {
			return tree;
		}
		
		public int getTier() {
			return tier;
		}
		
		public int getGrid() {
			return grid;
		}
		
		public int getRank() {
			return rank;
		}
		
		public int getRequiredLevel() {
			return requiredLevel;
		}
		
		public String getRequiredFaction() {
			return requiredFaction;
		}
		
		public String getRequiredProfession() {
			return requiredProfession;
		}
		
		private static String formatProfession(String profession) {
			switch (profession) {
				case "trader_dom":		return "trader_0a";
				case "trader_struct":	return "trader_0b";
				case "trader_mun":		return "trader_0c";
				case "trader_eng":		return "trader_0d";
				default:				return profession + "_1a";
			}
		}
		
	}
}
