package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod.adjust;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class LuckAdjustFunction implements Function<SkillModAdjust, Collection<SkillModAdjust>> {
	@Override
	public Collection<SkillModAdjust> apply(SkillModAdjust skillModAdjust) {
		int adjustBase = skillModAdjust.getBase();
		int adjustModifier = skillModAdjust.getModifier();
		
		int adjustBaseThreeToOne = (int) Math.ceil(adjustBase / 3f);
		int adjustModifierThreeToOne  = (int) Math.ceil(adjustModifier / 3f);
		
		int adjustBaseTwoToOne = (int) Math.ceil(adjustBase / 2f);
		int adjustModifierTwoToOne  = (int) Math.ceil(adjustModifier / 2f);
		
		return Arrays.asList(
				new SkillModAdjust("display_only_dodge", adjustBaseThreeToOne, adjustModifierThreeToOne),
				new SkillModAdjust("display_only_evasion", adjustBaseThreeToOne, adjustModifierThreeToOne),
				new SkillModAdjust("display_only_critical", adjustBaseThreeToOne, adjustModifierThreeToOne),
				new SkillModAdjust("display_only_strikethrough", adjustBaseTwoToOne, adjustModifierTwoToOne)
		
		);
	}
}
