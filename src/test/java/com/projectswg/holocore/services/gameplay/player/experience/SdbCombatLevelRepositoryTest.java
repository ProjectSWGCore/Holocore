package com.projectswg.holocore.services.gameplay.player.experience;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class SdbCombatLevelRepositoryTest {
	
	@Test
	public void canLoadLevels() {
		SdbCombatLevelRepository repository = new SdbCombatLevelRepository();
		Collection<CombatLevel> combatLevels = repository.getCombatLevels();
		int combatLevelCount = combatLevels.size();
		
		assertEquals(80, combatLevelCount);
	}
}