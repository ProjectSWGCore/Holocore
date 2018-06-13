package com.projectswg.holocore.services.support.npc;

import com.projectswg.holocore.services.support.npc.ai.AIService;
import com.projectswg.holocore.services.support.npc.spawn.SpawnerService;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		AIService.class,
		SpawnerService.class
})
public class NonPlayerCharacterManager extends Manager {
	
	public NonPlayerCharacterManager() {
		
	}
	
}
