package com.projectswg.holocore.services.gameplay.combat.loot;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		GrantLootService.class,
		LootGenerationService.class,
})
public class LootManager extends Manager {
	
	public LootManager() {
		
	}
	
}
