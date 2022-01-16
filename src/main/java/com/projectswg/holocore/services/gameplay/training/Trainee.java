package com.projectswg.holocore.services.gameplay.training;

import java.util.Set;

interface Trainee {
	Set<Skill> getCurrentSkills();
	void addSkill(Skill skill);
	void deductCredits(int credits);
	int getCredits();
	int getExperiencePoints(String type);
	void deductExperience(Experience requiredExperience);
}
