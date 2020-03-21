package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod.adjust;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class SingleModAdjustFunction implements Function<SkillModAdjust, Collection<SkillModAdjust>> {
	
	private final String skillModName;
	private final int multiplier;
	
	public SingleModAdjustFunction(String skillModName, int multiplier) {
		this.skillModName = skillModName;
		this.multiplier = multiplier;
	}
	
	public SingleModAdjustFunction(String skillModName) {
		this(skillModName, 1);
	}
	
	@Override
	public Collection<SkillModAdjust> apply(SkillModAdjust skillModAdjust) {
		return Collections.singleton(new SkillModAdjust(skillModName, skillModAdjust.getBase() * multiplier, skillModAdjust.getModifier() * multiplier));
	}
}
