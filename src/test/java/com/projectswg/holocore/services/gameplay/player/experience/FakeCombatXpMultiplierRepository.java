package com.projectswg.holocore.services.gameplay.player.experience;

import java.util.HashMap;
import java.util.Map;

public class FakeCombatXpMultiplierRepository implements CombatXpMultiplierRepository {
	
	private final Map<String, Integer> map;
	
	public FakeCombatXpMultiplierRepository() {
		map = new HashMap<>();
	}
	
	public void addMultiplier(String xpType, int multiplier) {
		map.put(xpType, multiplier);
	}
	
	@Override
	public int getMultiplier(String xpType) {
		return map.getOrDefault(xpType, 0);
	}
}
