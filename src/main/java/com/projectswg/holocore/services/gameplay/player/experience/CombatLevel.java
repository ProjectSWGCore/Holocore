package com.projectswg.holocore.services.gameplay.player.experience;

import java.util.Objects;

public class CombatLevel {
	private final int level;
	private final int requiredCombatXp;
	private final int healthAdded;
	
	public CombatLevel(int level, int requiredCombatXp, int healthAdded) {
		this.level = level;
		this.requiredCombatXp = requiredCombatXp;
		this.healthAdded = healthAdded;
	}
	
	public int getLevel() {
		return level;
	}
	
	public int getRequiredCombatXp() {
		return requiredCombatXp;
	}
	
	public int getHealthAdded() {
		return healthAdded;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CombatLevel that = (CombatLevel) o;
		return level == that.level;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(level);
	}
	
	@Override
	public String toString() {
		return "CombatLevel{" + "level=" + level + '}';
	}
}
