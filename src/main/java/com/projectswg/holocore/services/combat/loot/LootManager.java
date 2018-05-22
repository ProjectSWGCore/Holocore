package com.projectswg.holocore.services.combat.loot;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		LootService.class,
		RareLootService.class
})
public class LootManager extends Manager {
	
	public LootManager() {
		
	}
	
}
