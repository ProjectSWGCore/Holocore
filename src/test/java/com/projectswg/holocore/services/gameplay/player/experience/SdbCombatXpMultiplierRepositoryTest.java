package com.projectswg.holocore.services.gameplay.player.experience;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SdbCombatXpMultiplierRepositoryTest {
	
	private final SdbCombatXpMultiplierRepository repository = new SdbCombatXpMultiplierRepository();
	
	@Test
	public void multiplierIsAboveZero_whenXpTypeIsKnown() {
		// These come from the SDB
		String randomlySelectedXpType = "combat_general";
		int multiplierForRandomlySelectedXpType = 3;
		
		int multiplier = repository.getMultiplier(randomlySelectedXpType);
		
		assertEquals(multiplierForRandomlySelectedXpType, multiplier);
	}
	
	@Test
	public void multiplierIsZero_whenXpTypeIsUnknown() {
		String randomlySelectedXpType = "unknown_xp_type";
		
		int multiplier = repository.getMultiplier(randomlySelectedXpType);
		
		assertEquals(0, multiplier);
	}
}