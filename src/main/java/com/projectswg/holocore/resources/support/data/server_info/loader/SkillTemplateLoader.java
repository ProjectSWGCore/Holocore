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

public final class SkillTemplateLoader extends DataLoader {
	
	private final Map<String, SkillTemplateInfo> templateFromName;
	
	SkillTemplateLoader() {
		this.templateFromName = new HashMap<>();
	}
	
	@Nullable
	public SkillTemplateInfo getTemplateFromName(String name) {
		return templateFromName.get(name);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/skill/skill_template.sdb"))) {
			while (set.next()) {
				SkillTemplateInfo templateInfo = new SkillTemplateInfo(set);
				templateFromName.put(templateInfo.getName(), templateInfo);
			}
		}
	}
	
	public static class SkillTemplateInfo {
		
		private final String name;
		private final String startingName;
		private final String className;
		private final int uiPriority;
		private final String [] templates;
		private final boolean levelBased;
		private final String expertiseTrees;
		private final boolean respecAllowed;
		
		public SkillTemplateInfo(SdbResultSet set) {
			this.name = set.getText("template_name");
			this.startingName = set.getText("starting_template_name");
			this.className = set.getText("str_class_name");
			this.uiPriority = (int) set.getInt("user_interface_priority");
			this.templates = splitCsv(set.getText("template"));
			this.levelBased = set.getBoolean("level_based");
			this.expertiseTrees = set.getText("expertise_trees");
			this.respecAllowed = set.getInt("respec_allowed") != 0;
		}
		
		public String getName() {
			return name;
		}
		
		public String getStartingName() {
			return startingName;
		}
		
		public String getClassName() {
			return className;
		}
		
		public int getUiPriority() {
			return uiPriority;
		}
		
		public String [] getTemplates() {
			return templates.clone();
		}
		
		public boolean isLevelBased() {
			return levelBased;
		}
		
		public String getExpertiseTrees() {
			return expertiseTrees;
		}
		
		public boolean isRespecAllowed() {
			return respecAllowed;
		}
		
		private static String [] splitCsv(String val) {
			if (val.isBlank())
				return new String[0];
			return val.split(",");
		}
		
	}
}
