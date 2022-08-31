package com.projectswg.holocore.services.gameplay.training;

import java.util.*;

class TraineeFake implements Trainee {
	
	private final Set<Skill> skills;
	private final Map<String, Integer> experienceByType;
	private int credits;
	
	TraineeFake() {
		skills = new HashSet<>();
		experienceByType = new HashMap<>();
	}
	
	@Override
	public Set<Skill> getCurrentSkills() {
		return Collections.unmodifiableSet(skills);
	}
	
	@Override
	public void addSkill(Skill skill) {
		skills.add(skill);
	}
	
	@Override
	public void deductCredits(int credits) {
		this.credits -= credits;
	}
	
	public void addCredits(int credits) {
		this.credits += credits;
	}
	
	@Override
	public int getCredits() {
		return credits;
	}
	
	@Override
	public int getExperiencePoints(String type) {
		return experienceByType.getOrDefault(type, 0);
	}
	
	@Override
	public void deductExperience(Experience requiredExperience) {
		String requiredExperienceType = requiredExperience.getType();
		int requiredExperiencePoints = requiredExperience.getPoints();
		
		Integer currentExperiencePoints = experienceByType.getOrDefault(requiredExperienceType, 0);
		int nextExperiencePoints = currentExperiencePoints - requiredExperiencePoints;
		
		experienceByType.put(requiredExperienceType, nextExperiencePoints);
	}
	
	void putExperience(String type, int amount) {
		experienceByType.put(type, amount);
	}
}
