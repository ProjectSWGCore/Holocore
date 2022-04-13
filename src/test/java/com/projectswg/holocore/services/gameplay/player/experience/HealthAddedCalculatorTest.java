package com.projectswg.holocore.services.gameplay.player.experience;

import org.junit.Test;

import static org.junit.Assert.*;

public class HealthAddedCalculatorTest {
	
	private final CombatLevel combatLevel1 = new CombatLevel(1, 500, 0);
	private final CombatLevel combatLevel2 = new CombatLevel(2, 1000, 50);
	private final CombatLevel combatLevel3 = new CombatLevel(2, 6000, 75);
	private final HealthAddedCalculator healthAddedCalculator = new HealthAddedCalculator();
	
	@Test
	public void healthIsDecreased_whenLevelIsDecreased() {
		int healthChange = healthAddedCalculator.calculate(combatLevel3, combatLevel1);
		
		assertEquals(-75, healthChange);
	}
	
	@Test
	public void healthIsIncreased_whenLevelIsIncreased() {
		int healthChange = healthAddedCalculator.calculate(combatLevel2, combatLevel3);
		
		assertEquals(25, healthChange);
	}
	
	@Test
	public void healthIsUnchanged_whenLevelIsUnchanged() {
		int healthChange = healthAddedCalculator.calculate(combatLevel2, combatLevel2);
		
		assertEquals(0, healthChange);
	}
}