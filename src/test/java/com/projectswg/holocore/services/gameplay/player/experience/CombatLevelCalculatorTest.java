package com.projectswg.holocore.services.gameplay.player.experience;

import org.junit.Test;

import static org.junit.Assert.*;

public class CombatLevelCalculatorTest {
	private final CombatLevel combatLevel1 = new CombatLevel(1, 500, 0);
	private final CombatLevel combatLevel2 = new CombatLevel(2, 1000, 50);
	private final CombatLevel combatLevel3 = new CombatLevel(3, 6000, 75);
	
	private final FakeCombatLevelRepository repository = new FakeCombatLevelRepository();
	private final CombatLevelCalculator calculator = new CombatLevelCalculator(repository);
	
	@Test
	public void combatLevelIsSelected_whenRequiredXpIsMatchedExactly() {
		repository.addCombatLevel(combatLevel1);
		repository.addCombatLevel(combatLevel2);
		repository.addCombatLevel(combatLevel3);
		
		int exactXpForCombatLevel2 = combatLevel2.getRequiredCombatXp();
		CombatLevel combatLevel = calculator.calculate(exactXpForCombatLevel2);
		
		assertEquals(combatLevel2, combatLevel);
	}
	
	@Test
	public void smallestCombatLevelIsSelected_whenPlayerHasTooLittleXp() {
		repository.addCombatLevel(combatLevel1);
		repository.addCombatLevel(combatLevel2);
		repository.addCombatLevel(combatLevel3);
		
		int notEnoughXpForCombatLevel1 = 10;
		CombatLevel combatLevel = calculator.calculate(notEnoughXpForCombatLevel1);
		
		assertEquals(combatLevel1, combatLevel);
	}
	
	@Test
	public void biggestCombatLevelIsSelected_whenPlayerHasMuchXp() {
		repository.addCombatLevel(combatLevel1);
		repository.addCombatLevel(combatLevel2);
		repository.addCombatLevel(combatLevel3);
		
		int muchXp = 100_000;
		CombatLevel combatLevel = calculator.calculate(muchXp);
		
		assertEquals(combatLevel3, combatLevel);
	}
}