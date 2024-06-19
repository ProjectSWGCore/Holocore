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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class Training {
	
	private final SkillRepository skillRepository;
	
	Training(SkillRepository skillRepository) {
		this.skillRepository = skillRepository;
	}
	
	public Set<Skill> whatCanTraineeLearnNext(Profession profession, Trainee trainee) {
		Set<Skill> traineeSkills = trainee.getCurrentSkills();
		Set<Skill> traineeSkillsInProfession = traineeSkills.stream()
				.filter(skill -> isSkillBelongingToProfession(profession, skill))
				.collect(Collectors.toSet());
		
		if (traineeSkillsInProfession.isEmpty()) {
			Skill noviceSkill = getNoviceSkill(profession);
			Collection<Skill> prerequisiteSkills = noviceSkill.getPrerequisiteSkills();
			
			if (traineeSkills.containsAll(prerequisiteSkills)) {	// Novice boxes in elite professions have prerequisite skills
				return Collections.singleton(noviceSkill);
			}
			
			return Collections.emptySet();
		}
		
		Set<Skill> trainerSkills = skillRepository.getSkills().stream()
				.filter(skill -> isSkillBelongingToProfession(profession, skill))
				.collect(Collectors.toSet());
		
		return getSkillsOfferedByTrainerForTrainee(traineeSkillsInProfession, trainerSkills);
	}
	
	@NotNull
	private Set<Skill> getSkillsOfferedByTrainerForTrainee(Set<Skill> traineeSkills, Set<Skill> trainerSkills) {
		Set<Skill> skillsOfferedByTrainerForTrainee = new HashSet<>(trainerSkills);
		excludeSkillsTheTraineeAlreadyHas(traineeSkills, skillsOfferedByTrainerForTrainee);
		excludeSkillsTheTraineeIsMissingPrerequisitesFor(traineeSkills, skillsOfferedByTrainerForTrainee);
		return skillsOfferedByTrainerForTrainee;
	}
	
	private boolean isSkillBelongingToProfession(Profession profession, Skill skill) {
		String skillKey = skill.getKey();
		String professionName = profession.name();
		
		return skillKey.startsWith(professionName);
	}
	
	public Set<Skill> whatCanTraineeLearnRightNow(Profession profession, Trainee trainee) {
		Set<Skill> skillsOfferedByTrainerForTrainee = whatCanTraineeLearnNext(profession, trainee);
		
		return skillsOfferedByTrainerForTrainee.stream()
				.filter(skill -> traineeHasEnoughXPForSkill(trainee, skill))
				.collect(Collectors.toSet());
	}
	
	private void excludeSkillsTheTraineeIsMissingPrerequisitesFor(Set<Skill> traineeSkills, Set<Skill> skillsOfferedByTrainerForTrainee) {
		for (Skill skill : new HashSet<>(skillsOfferedByTrainerForTrainee)) {
			Collection<Skill> prerequisiteSkills = skill.getPrerequisiteSkills();
			
			if (!traineeSkills.containsAll(prerequisiteSkills)) {
				skillsOfferedByTrainerForTrainee.remove(skill);
			}
		}
	}
	
	private void excludeSkillsTheTraineeAlreadyHas(Set<Skill> traineeSkills, Set<Skill> skillsOfferedByTrainerForTrainee) {
		skillsOfferedByTrainerForTrainee.removeAll(traineeSkills);
	}
	
	public boolean traineeHasEnoughCredits(Trainee trainee, Skill skill) {
		return trainee.getCredits() >= skill.getRequiredCredits();
	}
	
	public void trainSkill(Trainee trainee, Skill skill) {
		boolean enoughAvailableSkillPoints = enoughAvailableSkillPoints(trainee, skill);
		boolean traineeHasEnoughCredits = traineeHasEnoughCredits(trainee, skill);
		
		if (enoughAvailableSkillPoints && traineeHasEnoughCredits) {
			deductCreditsFromTrainee(trainee, skill);
			deductExperienceFromTrainee(trainee, skill);
			
			trainee.addSkill(skill);
		}
	}
	
	public boolean enoughAvailableSkillPoints(Trainee trainee, Skill skill) {
		int consumedSkillPoints = getConsumedSkillPoints(trainee);
		int requiredSkillPoints = skill.getRequiredSkillPoints();
		
		return consumedSkillPoints + requiredSkillPoints < 250;
	}
	
	private int getConsumedSkillPoints(Trainee trainee) {
		Set<Skill> traineeSkills = trainee.getCurrentSkills();
		
		return traineeSkills.stream()
				.map(Skill::getRequiredSkillPoints)
				.mapToInt(Integer::intValue)
				.sum();
	}
	
	private void deductExperienceFromTrainee(Trainee trainee, Skill skill) {
		Experience requiredExperience = skill.getRequiredExperience();
		trainee.deductExperience(requiredExperience);
	}
	
	private void deductCreditsFromTrainee(Trainee trainee, Skill skill) {
		int requiredCredits = skill.getRequiredCredits();
		trainee.deductCredits(requiredCredits);
	}
	
	private boolean traineeHasEnoughXPForSkill(Trainee trainee, Skill skill) {
		Experience requiredExperience = skill.getRequiredExperience();
		int requiredExperiencePoints = requiredExperience.getPoints();
		int traineeExperiencePoints = trainee.getExperiencePoints(requiredExperience.getType());
		
		return traineeExperiencePoints >= requiredExperiencePoints;
	}
	
	private Skill getNoviceSkill(Profession trainerProfession) {
		return skillRepository.getSkill(trainerProfession.getNoviceSkillKey());
	}
}
