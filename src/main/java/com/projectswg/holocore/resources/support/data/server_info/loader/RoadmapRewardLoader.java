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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class RoadmapRewardLoader extends DataLoader {
	
	private final Map<String, RoadmapRewardInfo> rewardsBySkillName;
	
	RoadmapRewardLoader() {
		this.rewardsBySkillName = new HashMap<>();
	}
	
	@Nullable
	public RoadmapRewardInfo getRewardBySkillName(String skillName) {
		return rewardsBySkillName.get(skillName);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/roadmap/item_rewards.sdb"))) {
			while (set.next()) {
				RoadmapRewardInfo rewardInfo = new RoadmapRewardInfo(set);
				rewardsBySkillName.put(rewardInfo.getSkillName(), rewardInfo);
			}
		}
	}
	
	public static class RoadmapRewardInfo {
		
		private final String templateName;
		private final String skillName;
		private final String appearanceName;
		private final String stringId;
		private final String [] defaultItems;
		private final String [] wookieeItems;
		private final String [] ithorianItems;
		
		public RoadmapRewardInfo(SdbResultSet set) {
			this.templateName = set.getText("roadmap_template_name");
			this.skillName = set.getText("roadmap_skill_name");
			this.appearanceName = set.getText("appearance_name");
			this.stringId = set.getText("string_id");
			this.defaultItems = splitCsv(set.getText("item_default"));
			this.wookieeItems = splitCsv(set.getText("item_wookiee"));
			this.ithorianItems = splitCsv(set.getText("item_ithorian"));
		}
		
		@NotNull
		public String getTemplateName() {
			return templateName;
		}
		
		@NotNull
		public String getSkillName() {
			return skillName;
		}
		
		@NotNull
		public String getAppearanceName() {
			return appearanceName;
		}
		
		@NotNull
		public String getStringId() {
			return stringId;
		}
		
		@NotNull
		public String[] getDefaultItems() {
			return defaultItems.clone();
		}
		
		@NotNull
		public String[] getWookieeItems() {
			return wookieeItems.clone();
		}
		
		@NotNull
		public String[] getIthorianItems() {
			return ithorianItems.clone();
		}
		
		private static String [] splitCsv(String val) {
			if (val.isBlank())
				return new String[0];
			return val.split(",");
		}
		
	}
}
