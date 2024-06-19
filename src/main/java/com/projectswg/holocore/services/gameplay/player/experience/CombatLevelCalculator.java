/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
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
			int requiredCombatXp = combatLevel.requiredCombatXp();
			
			if (combatXp >= requiredCombatXp) {
				bestCandidate = combatLevel;
			}
		}
		
		return bestCandidate;
	}
	
	private CombatLevel getSmallestCombatLevel(List<CombatLevel> sortedCombatLevels) {
		return sortedCombatLevels.getFirst();
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
			int levelO1 = o1.level();
			int levelO2 = o2.level();
			
			return Integer.compare(levelO1, levelO2);
		}
	}
}
