package com.projectswg.holocore.services.gameplay.player.experience.skills;

import com.projectswg.holocore.intents.gameplay.player.badge.SetTitleIntent;
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
			grantParentSkills(skillData, parentSkillName, target);
			grantRequiredSkills(skillData, target);
		} else if (!target.hasSkill(parentSkillName) || !hasRequiredSkills(skillData, target)) {
			StandardLog.onPlayerError(this, target, "lacks required skill %s before being granted skill %s", parentSkillName, skillName);
			return;
		}
		
		grantSkill(skillData, skillName, target);
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
	
	private void grantParentSkills(SkillInfo skillData, String parentSkill, CreatureObject target) {
		if (skillData == null || parentSkill.isEmpty() || target.hasSkill(parentSkill))
			return;
		
		SkillInfo skillParent = DataLoader.skills().getSkillByName(parentSkill);
		if (skillParent == null)
			return;
		grantSkill(skillData, parentSkill, target);
		
		String grandParentSkill = skillData.getParent();
		grantParentSkills(skillParent, grandParentSkill, target);
	}
	
	private void grantRequiredSkills(SkillInfo skillData, CreatureObject target) {
		String[] requiredSkills = skillData.getSkillsRequired();
		if (requiredSkills == null)
			return;
		
		target.addSkill(requiredSkills);
	}
	
	private void grantSkill(SkillInfo skillData, String skillName, CreatureObject target) {
		target.addSkill(skillName);
		target.addAbility(skillData.getCommands());
		
		skillData.getSkillMods().forEach((skillModName, skillModValue) -> new SkillModIntent(skillModName, 0, skillModValue, target).broadcast());
		
		new GrantSkillIntent(GrantSkillIntent.IntentType.GIVEN, skillName, target, false).broadcast();
	}
	
}
