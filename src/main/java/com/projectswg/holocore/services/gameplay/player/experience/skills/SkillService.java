package com.projectswg.holocore.services.gameplay.player.experience.skills;

import com.projectswg.holocore.intents.gameplay.player.badge.SetTitleIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.SkillModIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader.SkillInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;

public class SkillService extends Service {
	
	public SkillService() {
		
	}
	
	@IntentHandler
	private void handleGrantSkillIntent(GrantSkillIntent gsi) {
		if (gsi.getIntentType() != GrantSkillIntent.IntentType.GRANT) {
			return;
		}
		
		String skillName = gsi.getSkillName();
		CreatureObject target = gsi.getTarget();
		SkillInfo skill = DataLoader.skills().getSkillByName(skillName);
		if (skill == null)
			return;
		
		grantSkill(target, skill, gsi.isGrantRequiredSkills());
	}
	
	@IntentHandler
	private void handleSetTitleIntent(SetTitleIntent sti) {
		String title = sti.getTitle();
		
		SkillInfo skillData = DataLoader.skills().getSkillByName(title);
		if (skillData == null) {
			// Might be a Collections title or someone playing tricks
			return;
		}
		
		if (!skillData.isTitle()) {
			// There's a skill with this name, but it doesn't grant a title
			return;
		}
		
		sti.getRequester().setTitle(title);
	}
	
	private void grantSkill(@NotNull CreatureObject target, @NotNull SkillInfo skill, boolean grantRequired) {
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
		
		SkillInfo skillInfo = DataLoader.skills().getSkillByName(skillName);
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
		
		SkillLoader skills = DataLoader.skills();
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
