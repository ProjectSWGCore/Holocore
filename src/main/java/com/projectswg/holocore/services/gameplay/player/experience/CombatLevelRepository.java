package com.projectswg.holocore.services.gameplay.player.experience;

import java.util.Collection;

public interface CombatLevelRepository {
	
	/**
	 *
	 * @return all combat levels. Sorted in no particular order.
	 */
	Collection<CombatLevel> getCombatLevels();
}
