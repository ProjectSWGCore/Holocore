package com.projectswg.holocore.services.gameplay.training;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AvailableSkillsWindow {
	
	private static final String GREEN = "\\#00FF00";
	private static final String RED = "\\#FF0000";
	private static final String WHITE = "\\#FFFFFF";
	
	public void show(String professionName, Player player) {
		Profession profession = new Profession(professionName);
		SkillsSdbSkillRepository skillRepository = new SkillsSdbSkillRepository();
		Trainee trainee = new TraineeImpl(player, skillRepository);
		Training training = new Training(skillRepository);
		
		SuiWindow window = createWindow(trainee, training, profession);
		
		window.display(player);
	}
	
	@NotNull
	private SuiWindow createWindow(Trainee trainee, Training training, Profession profession) {
		List<Skill> skills = new ArrayList<>(training.whatCanTraineeLearnRightNow(profession, trainee));
		SuiListBox listBox = new SuiListBox("Training", "Skills you can learn right now");
		
		addSkillsAsRows(trainee, training, skills, listBox);
		
		listBox.addOkButtonCallback("trainskill", (event, params) -> {
			int selectedRow = SuiListBox.getSelectedRow(params);
			
			if (isInvalidSelection(skills, selectedRow)) {
				return;
			}
			
			Skill skill = skills.get(selectedRow);
			
			training.trainSkill(trainee, skill);
		});
		
		return listBox;
	}
	
	private void addSkillsAsRows(Trainee trainee, Training training, List<Skill> skills, SuiListBox listBox) {
		for (Skill skill : skills) {
			String skillDisplayName = "@skl_n:" + skill.getKey();
			String requiredSkillPoints = getRequiredSkillPoints(training, skill, trainee);
			String requiredCredits = getRequiredCredits(training, trainee, skill);
			listBox.addListItem(skillDisplayName + " " + WHITE + "| " + requiredSkillPoints + " | " + requiredCredits);
		}
	}
	
	private String getRequiredSkillPoints(Training training, Skill skill, Trainee trainee) {
		int requiredSkillPoints = skill.getRequiredSkillPoints();
		boolean enoughAvailableSkillPoints = training.enoughAvailableSkillPoints(trainee, skill);
		
		String color;
		
		if (enoughAvailableSkillPoints) {
			color = GREEN;
		} else {
			color = RED;
		}
		
		return color + requiredSkillPoints + " skill points" + WHITE;
	}
	
	@NotNull
	private String getRequiredCredits(Training training, Trainee trainee, Skill skill) {
		String color;
		
		if (training.traineeHasEnoughCredits(trainee, skill)) {
			color = GREEN;
		} else {
			color = RED;
		}
		
		return color + skill.getRequiredCredits() + " credits" + WHITE;
	}
	
	private boolean isInvalidSelection(List<Skill> skills, int selectedRow) {
		return selectedRow < 0 || selectedRow >= skills.size();
	}
}
