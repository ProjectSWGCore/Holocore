package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.holocore.resources.support.data.server_info.loader.CombatXpMultiplierLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;

public class SdbCombatXpMultiplierRepository implements CombatXpMultiplierRepository {
	
	@Override
	public int getMultiplier(String xpType) {
		CombatXpMultiplierLoader combatXpMultiplierLoader = DataLoader.Companion.combatXpMultipliers();
		CombatXpMultiplierLoader.CombatXpMultiplierInfo combatXpMultiplierInfo = combatXpMultiplierLoader.getCombatXpMultiplier(xpType);
		
		if (combatXpMultiplierInfo != null) {
			return combatXpMultiplierInfo.getMultiplier();
		}
		
		return 0;
	}
}
