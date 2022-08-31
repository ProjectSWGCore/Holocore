package com.projectswg.holocore.intents.gameplay.player.experience.skills;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;

public class SurrenderSkillIntent extends Intent {
	
	private CreatureObject target;
	private String surrenderedSkill;
	
	private SurrenderSkillIntent(CreatureObject target, String surrenderedSkill) {
		this.target = target;
		this.surrenderedSkill = surrenderedSkill;
	}
	
	public CreatureObject getTarget() {
		return target;
	}
	
	public String getSurrenderedSkill() {
		return surrenderedSkill;
	}
	
	public static void broadcast(CreatureObject target, String surrenderedSkill) {
		new SurrenderSkillIntent(target, surrenderedSkill).broadcast();
	}
}
