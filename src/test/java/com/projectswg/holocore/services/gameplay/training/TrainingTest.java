package com.projectswg.holocore.services.gameplay.training;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TrainingTest {
	
	private SkillsSdbSkillRepository skillRepository;
	
	@BeforeEach
	public void setUp() throws Exception {
		skillRepository = new SkillsSdbSkillRepository();
	}
	
	@Test
	public void noviceIsOffered_whenTraineeHasNoSkillsInProfession() {
		Profession profession = getMedicProfession();
		Training training = new Training(skillRepository);
		Trainee trainee = new TraineeFake();
		
		Set<Skill> skillsOfferedByTrainerForTrainee = training.whatCanTraineeLearnRightNow(profession, trainee);
		
		Skill skill = skillsOfferedByTrainerForTrainee.iterator().next();
		assertEquals(profession.getNoviceSkillKey(), skill.getKey());
	}
	
	@Test
	public void skillIsExcludedFromOffered_whenTraineeLacksXP() {
		Profession profession = getMedicProfession();
		Training training = new Training(skillRepository);
		TraineeFake trainee = createNoviceMedic();
		trainee.putExperience("medical", 50);	// 50 XP isn't enough for any of the newbie medic skills
		
		Set<Skill> skillsOfferedByTrainerForTrainee = training.whatCanTraineeLearnRightNow(profession, trainee);
		
		assertTrue(skillsOfferedByTrainerForTrainee.isEmpty());
	}
	
	@Test
	public void skillIsOffered_whenTraineeHasEnoughXP() {
		Profession profession = getMedicProfession();
		Training training = new Training(skillRepository);
		TraineeFake trainee = createNoviceMedic();
		trainee.putExperience("medical", 1_000);
		
		Set<Skill> skillsOfferedByTrainerForTrainee = training.whatCanTraineeLearnRightNow(profession, trainee);
		
		assertEquals(3, skillsOfferedByTrainerForTrainee.size());
	}
	
	@Test
	public void skillIsNotOffered_whenTraineeIsMissingPrerequisiteSkills() {
		Profession profession = getMedicProfession();
		Training training = new Training(skillRepository);
		TraineeFake medic = createNoviceMedic();
		medic.putExperience("medical", 20_000);
		
		Set<Skill> skillsOfferedByTrainerForTrainee = training.whatCanTraineeLearnRightNow(profession, medic);
		
		assertEquals(3, skillsOfferedByTrainerForTrainee.size());
	}
	
	@Test
	public void skillForNoviceInEliteProfessionIsNotOffered_whenTraineeIsMissingPrerequisiteSkills() {
		Profession doctorProfession = new Profession("science_doctor");
		Training training = new Training(skillRepository);
		TraineeFake medic = createNoviceMedic();
		medic.putExperience("medical", 125_000);
		
		Set<Skill> skillsOfferedByTrainerForTrainee = training.whatCanTraineeLearnRightNow(doctorProfession, medic);
		
		assertTrue(skillsOfferedByTrainerForTrainee.isEmpty());
	}
	
	@Test
	public void traineeAcquiresSkill_whenSkillIsTrained() {
		Training training = new Training(skillRepository);
		TraineeFake medic = createNoviceMedic();
		medic.addCredits(1_500);
		medic.putExperience("medical", 1_500);
		Skill skillToTrain = getSkill("science_medic_injury_01");
		
		training.trainSkill(medic, skillToTrain);
		
		Set<Skill> currentSkills = medic.getCurrentSkills();
		assertTrue(currentSkills.contains(skillToTrain));
	}
	
	@Test
	public void creditsAreDeducted_whenSkillIsTrained() {
		Training training = new Training(skillRepository);
		TraineeFake trainee = createNoviceMedic();
		trainee.addCredits(1_500);
		trainee.putExperience("medical", 1_500);
		
		training.trainSkill(trainee, getSkill("science_medic_injury_01"));
		
		assertEquals(500, trainee.getCredits());
	}
	
	@Test
	public void skillIsNotTrained_whenRequiredCreditsAreLacking() {
		Training training = new Training(skillRepository);
		TraineeFake trainee = createNoviceMedic();
		trainee.addCredits(10);	// Not enough money
		trainee.putExperience("medical", 1_500);
		
		Skill requestedSkill = getSkill("science_medic_injury_01");
		training.trainSkill(trainee, requestedSkill);
		
		assertFalse(trainee.getCurrentSkills().contains(requestedSkill));
	}
	
	@Test
	public void skillIsNotTrained_whenRequiredSkillPointsAreLacking() {
		Training training = new Training(skillRepository);
		TraineeFake trainee = createTraineeWithManySkills();
		trainee.addCredits(100_000);	// More than enough credits
		trainee.putExperience("combat_meleespecialize_onehand", 750_000);	// More than enough XP
		
		Skill requestedSkill = getSkill("combat_1hsword_ability_04");
		training.trainSkill(trainee, requestedSkill);
		
		assertFalse(trainee.getCurrentSkills().contains(requestedSkill));
	}
	
	@Test
	public void experiencePointsAreDeducted_whenSkillIsTrained() {
		Training training = new Training(skillRepository);
		TraineeFake trainee = createNoviceMedic();
		trainee.addCredits(1_500);
		trainee.putExperience("medical", 1_500);
		
		training.trainSkill(trainee, getSkill("science_medic_injury_01"));
		
		assertEquals(500, trainee.getExperiencePoints("medical"));
	}
	
	private Skill getSkill(String skill) {
		return skillRepository.getSkill(skill);
	}
	
	@NotNull
	private TraineeFake createNoviceMedic() {
		TraineeFake medic = new TraineeFake();
		medic.addSkill(getSkill("science_medic_novice"));
		return medic;
	}
	
	@NotNull
	private TraineeFake createTraineeWithManySkills() {
		TraineeFake traineeWithManySkills = new TraineeFake();
		
		for (Skill brawlerSkill : getBrawlerSkills()) {
			traineeWithManySkills.addSkill(brawlerSkill);
		}
		
		for (Skill swordsmanSkill : getSwordsmanSkills()) {
			traineeWithManySkills.addSkill(swordsmanSkill);
		}
		
		for (Skill pikemanSkill : getPikemanSkills()) {
			traineeWithManySkills.addSkill(pikemanSkill);
		}
		
		for (Skill fencerSkill : getSomeFencerSkills()) {
			traineeWithManySkills.addSkill(fencerSkill);
		}
		
		return traineeWithManySkills;
	}
	
	private Set<Skill> getBrawlerSkills() {
		Set<Skill> skills = new HashSet<>();
		
		skills.add(getSkill("combat_brawler_novice"));
		skills.add(getSkill("combat_brawler_unarmed_01"));
		skills.add(getSkill("combat_brawler_unarmed_02"));
		skills.add(getSkill("combat_brawler_unarmed_03"));
		skills.add(getSkill("combat_brawler_unarmed_04"));
		skills.add(getSkill("combat_brawler_1handmelee_01"));
		skills.add(getSkill("combat_brawler_1handmelee_02"));
		skills.add(getSkill("combat_brawler_1handmelee_03"));
		skills.add(getSkill("combat_brawler_1handmelee_04"));
		skills.add(getSkill("combat_brawler_2handmelee_01"));
		skills.add(getSkill("combat_brawler_2handmelee_02"));
		skills.add(getSkill("combat_brawler_2handmelee_03"));
		skills.add(getSkill("combat_brawler_2handmelee_04"));
		skills.add(getSkill("combat_brawler_polearm_01"));
		skills.add(getSkill("combat_brawler_polearm_02"));
		skills.add(getSkill("combat_brawler_polearm_03"));
		skills.add(getSkill("combat_brawler_polearm_04"));
		skills.add(getSkill("combat_brawler_master"));
		
		return skills;
	}
	
	private Set<Skill> getSwordsmanSkills() {
		Set<Skill> skills = new HashSet<>();
		
		skills.add(getSkill("combat_2hsword_novice"));
		skills.add(getSkill("combat_2hsword_accuracy_01"));
		skills.add(getSkill("combat_2hsword_accuracy_02"));
		skills.add(getSkill("combat_2hsword_accuracy_03"));
		skills.add(getSkill("combat_2hsword_accuracy_04"));
		skills.add(getSkill("combat_2hsword_speed_01"));
		skills.add(getSkill("combat_2hsword_speed_02"));
		skills.add(getSkill("combat_2hsword_speed_03"));
		skills.add(getSkill("combat_2hsword_speed_04"));
		skills.add(getSkill("combat_2hsword_ability_01"));
		skills.add(getSkill("combat_2hsword_ability_02"));
		skills.add(getSkill("combat_2hsword_ability_03"));
		skills.add(getSkill("combat_2hsword_ability_04"));
		skills.add(getSkill("combat_2hsword_support_01"));
		skills.add(getSkill("combat_2hsword_support_02"));
		skills.add(getSkill("combat_2hsword_support_03"));
		skills.add(getSkill("combat_2hsword_support_04"));
		skills.add(getSkill("combat_2hsword_master"));
		
		return skills;
	}
	
	private Set<Skill> getPikemanSkills() {
		Set<Skill> skills = new HashSet<>();
		
		skills.add(getSkill("combat_polearm_novice"));
		skills.add(getSkill("combat_polearm_accuracy_01"));
		skills.add(getSkill("combat_polearm_accuracy_02"));
		skills.add(getSkill("combat_polearm_accuracy_03"));
		skills.add(getSkill("combat_polearm_accuracy_04"));
		skills.add(getSkill("combat_polearm_speed_01"));
		skills.add(getSkill("combat_polearm_speed_02"));
		skills.add(getSkill("combat_polearm_speed_03"));
		skills.add(getSkill("combat_polearm_speed_04"));
		skills.add(getSkill("combat_polearm_ability_01"));
		skills.add(getSkill("combat_polearm_ability_02"));
		skills.add(getSkill("combat_polearm_ability_03"));
		skills.add(getSkill("combat_polearm_ability_04"));
		skills.add(getSkill("combat_polearm_support_01"));
		skills.add(getSkill("combat_polearm_support_02"));
		skills.add(getSkill("combat_polearm_support_03"));
		skills.add(getSkill("combat_polearm_support_04"));
		skills.add(getSkill("combat_polearm_master"));
		
		return skills;
	}
	
	private Set<Skill> getSomeFencerSkills() {
		Set<Skill> skills = new HashSet<>();
		
		skills.add(getSkill("combat_1hsword_novice"));
		skills.add(getSkill("combat_1hsword_accuracy_01"));
		skills.add(getSkill("combat_1hsword_accuracy_02"));
		skills.add(getSkill("combat_1hsword_accuracy_03"));
		skills.add(getSkill("combat_1hsword_accuracy_04"));
		skills.add(getSkill("combat_1hsword_speed_01"));
		skills.add(getSkill("combat_1hsword_speed_02"));
		skills.add(getSkill("combat_1hsword_speed_03"));
		skills.add(getSkill("combat_1hsword_speed_04"));
		skills.add(getSkill("combat_1hsword_ability_01"));
		skills.add(getSkill("combat_1hsword_ability_02"));
		skills.add(getSkill("combat_1hsword_ability_03"));
		
		return skills;
	}
	
	@NotNull
	private Profession getMedicProfession() {
		return new Profession("science_medic");
	}
}
