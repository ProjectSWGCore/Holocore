package com.projectswg.holocore.services.support.global.commands;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		CommandQueueService.class,
		CommandExecutionService.class
})
public class CommandManager extends Manager {
	
	public CommandManager() {
		
	}
	
}
