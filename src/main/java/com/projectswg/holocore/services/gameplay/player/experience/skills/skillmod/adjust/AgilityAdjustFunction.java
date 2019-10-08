package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod.adjust;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class AgilityAdjustFunction implements Function<SkillModAdjust, Collection<SkillModAdjust>> {
	@Override
	public Collection<SkillModAdjust> apply(SkillModAdjust skillModAdjust) {
		int adjustBase = skillModAdjust.getBase();
		int adjustModifier = skillModAdjust.getModifier();
		
		int adjustBaseTwoToOne = (int) Math.ceil(adjustBase / 2f);
		int adjustModifierTwoToOne = (int) Math.ceil(adjustModifier / 2f);
		
		return Arrays.asList(
				new SkillModAdjust("display_only_dodge", adjustBase, adjustModifier),
				new SkillModAdjust("display_only_parry", adjustBaseTwoToOne, adjustModifierTwoToOne),
				new SkillModAdjust("display_only_evasion", adjustBase, adjustModifier)
		
		);
	}
}
