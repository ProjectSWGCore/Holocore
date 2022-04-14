package com.projectswg.holocore.services.gameplay.player.experience;

import java.util.Collection;

public class CombatXpCalculator {
	
	private final CombatXpMultiplierRepository repository;
	
	public CombatXpCalculator(CombatXpMultiplierRepository repository) {
		this.repository = repository;
	}
	
	public int calculate(Collection<Experience> experienceCollection) {
		return experienceCollection.stream()
				.mapToInt(this::applyMultiplier)
				.sum();
	}
	
	private int applyMultiplier(Experience experience) {
		String xpType = experience.getXpType();
		int multiplier = repository.getMultiplier(xpType);
		int experienceAmount = experience.getAmount();
		
		return experienceAmount * multiplier;
	}
}
