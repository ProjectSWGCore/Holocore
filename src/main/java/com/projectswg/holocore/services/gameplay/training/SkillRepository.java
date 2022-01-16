package com.projectswg.holocore.services.gameplay.training;

import java.util.Set;

interface SkillRepository {
	Skill getSkill(String key);
	
	Set<Skill> getSkills();
}
