package com.projectswg.holocore.services.gameplay.player.group;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		GroupService.class
})
public class GroupManager extends Manager {
	
	public GroupManager() {
		
	}
	
}
