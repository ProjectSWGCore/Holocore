package com.projectswg.holocore.services.support.npc.ai;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		AIService.class,
		AISchedulingService.class,
		AIMovementService.class
})
public class AIManager extends Manager {
	
	public AIManager() {
		
	}
	
}
