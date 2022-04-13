package com.projectswg.holocore.services.gameplay.player.experience;

import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class CombatXpCalculatorTest {
	
	private final FakeCombatXpMultiplierRepository repository = new FakeCombatXpMultiplierRepository();
	private final CombatXpCalculator calculator = new CombatXpCalculator(repository);
	
	@Test
	public void xpTypeIsIgnored_whenMultiplierIsUndefined() {
		Collection<Experience> experienceCollection = List.of(new Experience("has_no_multiplier", 100));
		
		int combatXp = calculator.calculate(experienceCollection);
		
		assertEquals(0, combatXp);
	}
	
	@Test
	public void xpTypeCounts_whenMultiplierIsDefined() {
		repository.addMultiplier("has_multiplier", 1);
		Collection<Experience> experienceCollection = List.of(new Experience("has_multiplier", 100));
		int combatXp = calculator.calculate(experienceCollection);
		
		assertEquals(100, combatXp);
	}
	
	@Test
	public void xpIsMultiplied_whenMultiplierForTypeIsAboveOne() {
		repository.addMultiplier("has_multiplier", 5);
		Collection<Experience> experienceCollection = List.of(new Experience("has_multiplier", 100));
		int combatXp = calculator.calculate(experienceCollection);
		
		assertEquals(500, combatXp);
	}
}