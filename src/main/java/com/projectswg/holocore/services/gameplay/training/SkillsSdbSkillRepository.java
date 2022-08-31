package com.projectswg.holocore.services.gameplay.training;

import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader;
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
		String[] skillsRequired = skillInfo.getSkillsRequired();
		
		for (String skillRequired : skillsRequired) {
			SkillLoader.SkillInfo requiredSkillInfo = skillLoader.getSkillByName(skillRequired);
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
