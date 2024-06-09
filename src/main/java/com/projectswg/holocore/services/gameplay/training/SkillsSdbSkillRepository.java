/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.training;

import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class SkillsSdbSkillRepository implements SkillRepository {

	private final SkillLoader skillLoader;

	SkillsSdbSkillRepository() {
		skillLoader = DataLoader.Companion.skills();
	}

	@Override
	public Skill getSkill(String key) {
		SkillLoader.SkillInfo skillInfo = DataLoader.Companion.skills().getSkillByName(key);
		if (skillInfo == null)
			return null;

		if (!skillInfo.isProfession()) {
			Skill skill = convertSkillInfoToSkill(skillInfo);
			addPrerequisiteSkillsToSkill(skillInfo, skill);
			return skill;
		}

		return null;
	}

	@Override
	public Set<Skill> getSkills() {
		Collection<SkillLoader.SkillInfo> skillInfos = skillLoader.getSkills();
		Set<Skill> skills = new HashSet<>();

		for (SkillLoader.SkillInfo skillInfo : skillInfos) {
			if (!skillInfo.isProfession()) {
				Skill skill = convertSkillInfoToSkill(skillInfo);
				addPrerequisiteSkillsToSkill(skillInfo, skill);

				skills.add(skill);
			}
		}

		return skills;
	}

	private void addPrerequisiteSkillsToSkill(SkillLoader.SkillInfo skillInfo, Skill skill) {
		for (String skillRequired : skillInfo.getSkillsRequired()) {
			SkillLoader.SkillInfo requiredSkillInfo = skillLoader.getSkillByName(skillRequired);
			if (requiredSkillInfo == null) {
				Log.w("[SkillsSdbSkillRepository] Invalid skill name: %s", skillRequired);
				continue;
			}
			Skill prerequisiteSkill = convertSkillInfoToSkill(requiredSkillInfo);

			skill.addPrerequisiteSkill(prerequisiteSkill);
		}
	}

	@NotNull
	private Skill convertSkillInfoToSkill(SkillLoader.SkillInfo skillInfo) {
		Experience requiredExperience = new Experience(skillInfo.getXpCost(), skillInfo.getXpType());

		return new Skill(skillInfo.getName(), requiredExperience, skillInfo.getMoneyRequired(), skillInfo.getPointsRequired());
	}
}
