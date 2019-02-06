package com.projectswg.holocore.services.gameplay.player.experience.skills;

import com.projectswg.holocore.intents.gameplay.player.badge.SetTitleIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.LevelChangedIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.SkillModIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader.SkillInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

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
		SkillInfo skillData = DataLoader.skills().getSkillByName(skillName);
		if (skillData == null)
			return;
		
		String parentSkillName = skillData.getParent();
		
		if (gsi.isGrantRequiredSkills()) {
			grantParentSkills(parentSkillName, target);
			grantRequiredSkills(skillData, target);
		} else if (!target.hasSkill(parentSkillName) || !hasRequiredSkills(skillData, target)) {
			StandardLog.onPlayerError(this, target, "lacks required skill %s before being granted skill %s", parentSkillName, skillName);
			return;
		}
		
		grantSkill(skillData, target);
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
	
	@IntentHandler
	private void handleLevelChangedIntent(LevelChangedIntent lci) {
		
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
	
	private void grantParentSkills(String skillName, CreatureObject target) {
		if (skillName.isEmpty() || target.hasSkill(skillName))
			return; // Nothing to do here
		
		SkillInfo skillInfo = DataLoader.skills().getSkillByName(skillName);
		if (skillInfo == null) {
			StandardLog.onPlayerTrace(this, target, "requires an invalid parent skill: %s", skillName);
			return;
		}
		
		grantParentSkills(skillInfo.getParent(), target);
		grantSkill(skillInfo, target);
	}
	
	private void grantRequiredSkills(SkillInfo skillData, CreatureObject target) {
		String[] requiredSkills = skillData.getSkillsRequired();
		if (requiredSkills == null)
			return;
		
		target.addSkill(requiredSkills);
	}
	
	private void grantSkill(SkillInfo skillData, CreatureObject target) {
		target.addSkill(skillData.getName());
		target.addAbility(skillData.getCommands());
		
		skillData.getSkillMods().forEach((skillModName, skillModValue) -> new SkillModIntent(skillModName, 0, skillModValue, target).broadcast());
		new GrantSkillIntent(GrantSkillIntent.IntentType.GIVEN, skillData.getName(), target, false).broadcast();
	}
	
}
