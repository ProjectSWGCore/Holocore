package com.projectswg.holocore.services.gameplay.player.collections;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		CollectionService.class
})
public class CollectionManager extends Manager {
	
	public CollectionManager() {
		
	}
	
}
