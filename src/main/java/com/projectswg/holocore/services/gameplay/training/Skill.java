package com.projectswg.holocore.services.gameplay.training;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

class Skill {
	private final String key;
	private final Experience requiredExperience;
	private final Collection<Skill> prerequisiteSkills;
	private final int requiredCredits;
	private final int requiredSkillPoints;
	
	Skill(String key, Experience requiredExperience, int requiredCredits, int requiredSkillPoints) {
		this.key = key;
		this.requiredExperience = requiredExperience;
		this.requiredSkillPoints = requiredSkillPoints;
		this.prerequisiteSkills = new ArrayList<>();
		this.requiredCredits = requiredCredits;
	}
	
	public String getKey() {
		return key;
	}
	
	public Experience getRequiredExperience() {
		return requiredExperience;
	}
	
	public void addPrerequisiteSkill(Skill skill) {
		prerequisiteSkills.add(skill);
	}
	
	public Collection<Skill> getPrerequisiteSkills() {
		return new ArrayList<>(prerequisiteSkills);
	}
	
	public int getRequiredCredits() {
		return requiredCredits;
	}
	
	public int getRequiredSkillPoints() {
		return requiredSkillPoints;
	}
	
	@Override
	public String toString() {
		return "Skill{" + "key='" + key + '\'' + '}';
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Skill skill = (Skill) o;
		return key.equals(skill.key);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(key);
	}
}
