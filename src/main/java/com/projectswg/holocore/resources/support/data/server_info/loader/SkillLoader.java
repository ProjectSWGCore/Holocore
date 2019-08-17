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
import java.util.*;

public final class SkillLoader extends DataLoader {
	
	private final Map<String, SkillInfo> skillsByName;
	
	SkillLoader() {
		this.skillsByName = new HashMap<>();
	}
	
	@Nullable
	public SkillInfo getSkillByName(String name) {
		return skillsByName.get(name);
	}
	
	@NotNull
	public Collection<SkillInfo> getSkills() {
		return Collections.unmodifiableCollection(skillsByName.values());
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/skill/skills.sdb"))) {
			while (set.next()) {
				SkillInfo skills = new SkillInfo(set);
				skillsByName.put(skills.getName(), skills);
			}
		}
	}
	
	@SuppressWarnings("ClassWithTooManyFields")
	public static class SkillInfo {
		
		private final String name;
		private final String parent;
		private final String graphType;
		private final boolean godOnly;
		private final boolean title;
		private final boolean profession;
		private final boolean hidden;
		private final int moneyRequired;
		private final int pointsRequired;
		private final int skillsRequiredCount;
		private final String [] skillsRequired;
		private final String [] preclusionSkills;
		private final String xpType;
		private final int xpCost;
		private final int xpCap;
		private final String [] missionsRequired;
		private final int apprenticeshipsRequired;
		private final String [] statsRequired;
		private final String [] speciesRequired;
		private final String jediStateRequired;
		private final String skillAbility;
		private final String [] commands;
		private final Map<String, Integer> skillMods;
		private final String [] schematicsGranted;
		private final String [] schematicsRevoked;
		private final boolean searchable;
		private final int ender;
		
		public SkillInfo(SdbResultSet set) {
			this.name = set.getText("name");
			this.parent = set.getText("parent");
			this.graphType = set.getText("graph_type");
			this.godOnly = set.getBoolean("god_only");
			this.title = set.getBoolean("is_title");
			this.profession = set.getBoolean("is_profession");
			this.hidden = set.getBoolean("is_hidden");
			this.moneyRequired = (int) set.getInt("money_required");
			this.pointsRequired = (int) set.getInt("points_required");
			this.skillsRequiredCount = (int) set.getInt("skills_required_count");
			this.skillsRequired = splitCsv(set.getText("skills_required"));
			this.preclusionSkills = splitCsv(set.getText("preclusion_skills"));
			this.xpType = set.getText("xp_type");
			this.xpCost = (int) set.getInt("xp_cost");
			this.xpCap = (int) set.getInt("xp_cap");
			this.missionsRequired = splitCsv(set.getText("missions_required"));
			this.apprenticeshipsRequired = (int) set.getInt("apprenticeships_required");
			this.statsRequired = splitCsv(set.getText("stats_required"));
			this.speciesRequired = splitCsv(set.getText("species_required"));
			this.jediStateRequired = set.getText("jedi_state_required");
			this.skillAbility = set.getText("skill_ability");
			this.commands = splitCsv(set.getText("commands"));
			this.skillMods = createSkillModMap(splitCsv(set.getText("skill_mods")));
			this.schematicsGranted = splitCsv(set.getText("schematics_granted"));
			this.schematicsRevoked = splitCsv(set.getText("schematics_revoked"));
			this.searchable = set.getBoolean("searchable");
			this.ender = (int) set.getInt("ender");
		}
		
		public String getName() {
			return name;
		}
		
		public String getParent() {
			return parent;
		}
		
		public String getGraphType() {
			return graphType;
		}
		
		public boolean isGodOnly() {
			return godOnly;
		}
		
		public boolean isTitle() {
			return title;
		}
		
		public boolean isProfession() {
			return profession;
		}
		
		public boolean isHidden() {
			return hidden;
		}
		
		public int getMoneyRequired() {
			return moneyRequired;
		}
		
		public int getPointsRequired() {
			return pointsRequired;
		}
		
		public int getSkillsRequiredCount() {
			return skillsRequiredCount;
		}
		
		public String[] getSkillsRequired() {
			return skillsRequired.clone();
		}
		
		public String[] getPreclusionSkills() {
			return preclusionSkills.clone();
		}
		
		public String getXpType() {
			return xpType;
		}
		
		public int getXpCost() {
			return xpCost;
		}
		
		public int getXpCap() {
			return xpCap;
		}
		
		public String[] getMissionsRequired() {
			return missionsRequired.clone();
		}
		
		public int getApprenticeshipsRequired() {
			return apprenticeshipsRequired;
		}
		
		public String[] getStatsRequired() {
			return statsRequired.clone();
		}
		
		public String[] getSpeciesRequired() {
			return speciesRequired.clone();
		}
		
		public String getJediStateRequired() {
			return jediStateRequired;
		}
		
		public String getSkillAbility() {
			return skillAbility;
		}
		
		public String[] getCommands() {
			return commands.clone();
		}
		
		public Map<String, Integer> getSkillMods() {
			return Collections.unmodifiableMap(skillMods);
		}
		
		public String[] getSchematicsGranted() {
			return schematicsGranted.clone();
		}
		
		public String[] getSchematicsRevoked() {
			return schematicsRevoked.clone();
		}
		
		public boolean isSearchable() {
			return searchable;
		}
		
		public int getEnder() {
			return ender;
		}
		
		private static String [] splitCsv(String str) {
			if (str.isEmpty())
				return new String[0];
			else if (str.indexOf(',') == -1)
				return new String[]{str};
			return str.split(",");
		}
		
		private static Map<String, Integer> createSkillModMap(String [] elements) {
			Map<String, Integer> skillMods = new HashMap<>();
			for (String element : elements) {
				String [] split = element.split("=", 2);
				if (split.length < 2)
					continue;
				skillMods.put(split[0], Integer.valueOf(split[1]));
			}
			return skillMods;
		}
		
	}
}
