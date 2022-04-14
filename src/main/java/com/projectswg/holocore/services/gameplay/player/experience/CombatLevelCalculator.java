package com.projectswg.holocore.services.gameplay.player.experience;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class CombatLevelCalculator {
	
	private final CombatLevelRepository repository;
	
	public CombatLevelCalculator(CombatLevelRepository repository) {
		this.repository = repository;
	}
	
	public CombatLevel calculate(int combatXp) {
		List<CombatLevel> sortedCombatLevels = getSortedCombatLevels();
		CombatLevel bestCandidate = getSmallestCombatLevel(sortedCombatLevels);
		
		for (CombatLevel combatLevel : sortedCombatLevels) {
			int requiredCombatXp = combatLevel.getRequiredCombatXp();
			
			if (combatXp >= requiredCombatXp) {
				bestCandidate = combatLevel;
			}
		}
		
		return bestCandidate;
	}
	
	private CombatLevel getSmallestCombatLevel(List<CombatLevel> sortedCombatLevels) {
		return sortedCombatLevels.get(0);
	}
	
	@NotNull
	private List<CombatLevel> getSortedCombatLevels() {
		Collection<CombatLevel> combatLevels = repository.getCombatLevels();
		List<CombatLevel> sortedCombatLevels = new ArrayList<>(combatLevels);
		sortedCombatLevels.sort(new CombatLevelComparator());
		return sortedCombatLevels;
	}
	
	private static class CombatLevelComparator implements Comparator<CombatLevel>  {
		
		@Override
		public int compare(CombatLevel o1, CombatLevel o2) {
			return sortAscByLevel(o1, o2);
		}
		
		private int sortAscByLevel(CombatLevel o1, CombatLevel o2) {
			int levelO1 = o1.getLevel();
			int levelO2 = o2.getLevel();
			
			return Integer.compare(levelO1, levelO2);
		}
	}
}
