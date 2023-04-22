/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.player.experience.skills;

import com.projectswg.holocore.intents.gameplay.player.badge.GrantBadgeIntent;
import com.projectswg.holocore.intents.gameplay.player.badge.SetTitleIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.SkillModIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.SurrenderSkillIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.BadgeLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader.SkillInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.services.gameplay.player.experience.*;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SkillService extends Service {

	private static final int SKILL_POINT_CAP = 250;
	private final CombatXpCalculator combatXpCalculator;
	private final CombatLevelCalculator combatLevelCalculator;
	private final HealthAddedCalculator healthAddedCalculator;

	public SkillService() {
		combatXpCalculator = new CombatXpCalculator(new SdbCombatXpMultiplierRepository());
		combatLevelCalculator = new CombatLevelCalculator(new SdbCombatLevelRepository());
		healthAddedCalculator = new HealthAddedCalculator();
	}
	
	@IntentHandler
	private void handleGrantSkillIntent(GrantSkillIntent gsi) {
		if (gsi.getIntentType() != GrantSkillIntent.IntentType.GRANT) {
			return;
		}
		
		String skillName = gsi.getSkillName();
		CreatureObject target = gsi.getTarget();
		SkillInfo skill = DataLoader.Companion.skills().getSkillByName(skillName);
		if (skill == null)
			return;
		
		CombatLevel oldCombatLevel = getCombatLevel(target);
		grantSkill(target, skill, gsi.isGrantRequiredSkills());
		CombatLevel newCombatLevel = getCombatLevel(target);
		
		boolean levelChanged = !oldCombatLevel.equals(newCombatLevel);
		
		if (levelChanged) {
			changeLevel(target, oldCombatLevel, newCombatLevel);
		}
		
		BadgeLoader.BadgeInfo badgeFromKey = DataLoader.Companion.badges().getBadgeFromKey(skillName);
		
		if (badgeFromKey != null) {
			GrantBadgeIntent.broadcast(target, skillName);
		}
	}
	
	private void adjustHealth(CreatureObject target, CombatLevel oldCombatLevel, CombatLevel newCombatLevel) {
		int healthChange = healthAddedCalculator.calculate(oldCombatLevel, newCombatLevel);
		target.setLevelHealthGranted(newCombatLevel.getHealthAdded());
		target.setMaxHealth(target.getMaxHealth() + healthChange);
		target.setHealth(target.getMaxHealth());
	}
	
	private CombatLevel getCombatLevel(CreatureObject target) {
		Collection<Experience> experienceCollection = getExperienceFromTrainedSkills(target);
		int oldCombatLevelXpFromTrainedSkills = combatXpCalculator.calculate(experienceCollection);
		return combatLevelCalculator.calculate(oldCombatLevelXpFromTrainedSkills);
	}
	
	@NotNull
	private Collection<Experience> getExperienceFromTrainedSkills(CreatureObject target) {
		Set<String> trainedSkills = target.getSkills();
		Collection<Experience> experienceCollection = new ArrayList<>();
		for (String trainedSkill : trainedSkills) {
			Experience experience = convertTrainedSkillToExperience(trainedSkill);
			
			if (experience != null) {
				experienceCollection.add(experience);
			}
		}
		
		return experienceCollection;
	}
	
	private Experience convertTrainedSkillToExperience(String trainedSkill) {
		SkillInfo trainedSkillInfo = DataLoader.Companion.skills().getSkillByName(trainedSkill);
		
		if (trainedSkillInfo != null) {
			String xpType = trainedSkillInfo.getXpType();
			int xpCost = trainedSkillInfo.getXpCost();
			
			return new Experience(xpType, xpCost);
		}
		
		return null;
	}
	
	@IntentHandler
	private void handleSetTitleIntent(SetTitleIntent sti) {
		PlayerObject requester = sti.getRequester();
		String title = sti.getTitle();
		
		if (title.isBlank()) {
			requester.setTitle(title);
			return;
		}
		
		SkillInfo skillData = DataLoader.Companion.skills().getSkillByName(title);
		if (skillData == null) {
			// Might be a Collections title or someone playing tricks
			return;
		}
		
		if (!skillData.isTitle()) {
			// There's a skill with this name, but it doesn't grant a title
			return;
		}
		
		CreatureObject creatureObject = Objects.requireNonNull(requester.getOwner()).getCreatureObject();
		Set<String> skills = creatureObject.getSkills();
		
		if (skills.contains(title)) {
			requester.setTitle(title);
		}
	}

	@IntentHandler
	private void handleSurrenderSkillIntent(SurrenderSkillIntent ssi) {
		CreatureObject target = ssi.getTarget();
		String surrenderedSkill = ssi.getSurrenderedSkill();

		if (!target.hasSkill(surrenderedSkill)) {
			// They don't even have this skill. Do nothing.

			Log.w("%s could not surrender skill %s because they do not have it", target, surrenderedSkill);

			return;
		}

		Optional<String[]> dependentSkills = target.getSkills().stream()
				.map(skill -> DataLoader.Companion.skills().getSkillByName(skill))
				.map(SkillInfo::getSkillsRequired)
				.filter(requiredSkills -> {
					for (String requiredSkill : requiredSkills) {
						if (requiredSkill.equals(surrenderedSkill)) {
							return true;
						}
					}

					return false;
				})
				.findAny();

		if (dependentSkills.isPresent()) {
			Log.d("%s could not surrender skill %s because these skills depend on it: ",
					target, Arrays.toString(dependentSkills.get()));
			return;
		}

		SkillInfo skillInfo = DataLoader.Companion.skills().getSkillByName(surrenderedSkill);
		
		CombatLevel oldCombatLevel = getCombatLevel(target);
		
		target.removeSkill(surrenderedSkill);
		target.removeCommands(skillInfo.getCommands());
		skillInfo.getSkillMods().forEach((skillModName, skillModValue) -> new SkillModIntent(skillModName, 0, -skillModValue, target).broadcast());
		
		CombatLevel newCombatLevel = getCombatLevel(target);
		
		boolean levelChanged = !oldCombatLevel.equals(newCombatLevel);
		
		if (levelChanged) {
			changeLevel(target, oldCombatLevel, newCombatLevel);
		}
	}
	
	private void changeLevel(CreatureObject target, CombatLevel oldCombatLevel, CombatLevel newCombatLevel) {
		target.setLevel(newCombatLevel.getLevel());
		adjustHealth(target, oldCombatLevel, newCombatLevel);
	}
	
	private void grantSkill(@NotNull CreatureObject target, @NotNull SkillInfo skill, boolean grantRequired) {
		int pointsRequired = skill.getPointsRequired();
		int skillPointsSpent = skillPointsSpent(target);

		if (skillPointsSpent(target) + pointsRequired > SKILL_POINT_CAP) {
			int missingPoints = pointsRequired - (SKILL_POINT_CAP - skillPointsSpent);

			Log.d("%s cannot learn %s because they lack %d skill points", target, skill.getName(), missingPoints);
			return;
		}


		String parentSkillName = skill.getParent();
		
		if (grantRequired) {
			grantParentSkills(parentSkillName, target);
			grantRequiredSkills(skill, target);
		}
		
		grantSkill(target, skill);
	}
	
	private void grantParentSkills(String skillName, CreatureObject target) {
		if (skillName.isEmpty() || target.hasSkill(skillName))
			return; // Nothing to do here
		
		SkillInfo skillInfo = DataLoader.Companion.skills().getSkillByName(skillName);
		if (skillInfo == null) {
			StandardLog.onPlayerTrace(this, target, "requires an invalid parent skill: %s", skillName);
			return;
		}
		
		grantParentSkills(skillInfo.getParent(), target);
		grantSkill(target, skillInfo);
	}
	
	private void grantRequiredSkills(SkillInfo skillData, CreatureObject target) {
		String[] requiredSkills = skillData.getSkillsRequired();
		if (requiredSkills == null)
			return;
		
		SkillLoader skills = DataLoader.Companion.skills();
		for (String requiredSkillName : requiredSkills) {
			SkillInfo requiredSkill = skills.getSkillByName(requiredSkillName);
			if (requiredSkill != null)
				grantSkill(target, requiredSkill, true);
		}
	}
	
	private void grantSkill(CreatureObject target, SkillInfo skill) {
		if ((!skill.getParent().isEmpty() && !target.hasSkill(skill.getParent())) || !hasRequiredSkills(skill, target)) {
			StandardLog.onPlayerError(this, target, "lacks required skill %s before being granted skill %s", skill.getParent(), skill.getName());
			return;
		}
		if (!target.addSkill(skill.getName()))
			return;
		target.addCommand(skill.getCommands());
		
		skill.getSkillMods().forEach((skillModName, skillModValue) -> new SkillModIntent(skillModName, skillModValue, 0, target).broadcast());
		new GrantSkillIntent(GrantSkillIntent.IntentType.GIVEN, skill.getName(), target, false).broadcast();
	}

	private int skillPointsSpent(CreatureObject creature) {
		return creature.getSkills().stream()
				.map(skillName -> DataLoader.Companion.skills().getSkillByName(skillName))
				.map(SkillInfo::getPointsRequired)
				.mapToInt(Integer::intValue)
				.sum();
	}

	private boolean hasRequiredSkills(SkillInfo skillData, CreatureObject creatureObject) {
		String[] requiredSkills = skillData.getSkillsRequired();
		if (requiredSkills == null)
			return true;
		
		for (String required : requiredSkills) {
			if (!creatureObject.hasSkill(required))
				return false;
		}
		return true;
	}
	
}
