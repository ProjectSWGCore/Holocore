package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod.adjust;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class StrengthAdjustFunction implements Function<SkillModAdjust, Collection<SkillModAdjust>> {
	@Override
	public Collection<SkillModAdjust> apply(SkillModAdjust skillModAdjust) {
		int adjustBase = skillModAdjust.getBase();
		int adjustModifier = skillModAdjust.getModifier();
		
		int adjustBaseTwoToOne = (int) Math.ceil(adjustBase / 2f);
		int adjustModifierTwoToOne  = (int) Math.ceil(adjustModifier / 2f);
		
		return Collections.singleton(new SkillModAdjust("display_only_block", adjustBaseTwoToOne, adjustModifierTwoToOne));
	}
}
