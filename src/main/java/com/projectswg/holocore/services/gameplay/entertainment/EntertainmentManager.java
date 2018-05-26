package com.projectswg.holocore.services.gameplay.entertainment;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		EntertainmentService.class
})
public class EntertainmentManager extends Manager {
	
	public EntertainmentManager() {
		
	}
	
}
