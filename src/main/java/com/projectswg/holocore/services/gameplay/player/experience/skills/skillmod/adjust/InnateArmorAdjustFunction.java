package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod.adjust;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class InnateArmorAdjustFunction implements Function<SkillModAdjust, Collection<SkillModAdjust>> {
	
	@Override
	public Collection<SkillModAdjust> apply(SkillModAdjust skillModAdjust) {
		int adjustBase = skillModAdjust.getBase();
		int adjustModifier = skillModAdjust.getModifier();
		
		return Arrays.asList(
				new SkillModAdjust("kinetic", adjustBase, adjustModifier),
				new SkillModAdjust("energy", adjustBase, adjustModifier),
				new SkillModAdjust("heat", adjustBase, adjustModifier),
				new SkillModAdjust("cold", adjustBase, adjustModifier),
				new SkillModAdjust("acid", adjustBase, adjustModifier),
				new SkillModAdjust("electricity", adjustBase, adjustModifier)
		);
	}
}
