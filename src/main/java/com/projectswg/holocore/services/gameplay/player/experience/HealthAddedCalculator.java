package com.projectswg.holocore.services.gameplay.player.experience;

public class HealthAddedCalculator {
	public int calculate(CombatLevel oldCombatLevel, CombatLevel newCombatLevel) {
		int oldHealthAdded = oldCombatLevel.getHealthAdded();
		int newHealthAdded = newCombatLevel.getHealthAdded();
		
		return newHealthAdded - oldHealthAdded;
	}
}
