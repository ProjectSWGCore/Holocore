package com.projectswg.holocore.services.gameplay.player.badge;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		BadgeService.class,
		ExplorationBadgeService.class
})
public class BadgeManager extends Manager {
	
	public BadgeManager() {
		
	}
	
}
