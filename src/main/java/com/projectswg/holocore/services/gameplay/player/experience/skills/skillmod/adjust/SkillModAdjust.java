package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod.adjust;

public class SkillModAdjust {
	private final String name;
	private final int base;
	private final int modifier;
	
	public SkillModAdjust(String name, int base, int modifier) {
		this.name = name;
		this.base = base;
		this.modifier = modifier;
	}
	
	public String getName() {
		return name;
	}
	
	public int getBase() {
		return base;
	}
	
	public int getModifier() {
		return modifier;
	}
}
