package com.projectswg.holocore.services.gameplay.player.experience;

import java.util.Collection;
import java.util.LinkedHashSet;

class FakeCombatLevelRepository implements CombatLevelRepository {
	
	private final Collection<CombatLevel> combatLevels;
	
	public FakeCombatLevelRepository() {
		combatLevels = new LinkedHashSet<>();
	}
	
	public void addCombatLevel(CombatLevel combatLevel) {
		combatLevels.add(combatLevel);
	}
	
	@Override
	public Collection<CombatLevel> getCombatLevels() {
		return combatLevels;
	}
}
