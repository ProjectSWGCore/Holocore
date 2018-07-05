package com.projectswg.holocore.services.support.global;

import com.projectswg.holocore.services.support.global.chat.ChatManager;
import com.projectswg.holocore.services.support.global.commands.CommandManager;
import com.projectswg.holocore.services.support.global.health.ServerHealthService;
import com.projectswg.holocore.services.support.global.network.NetworkClientService;
import com.projectswg.holocore.services.support.global.zone.ZoneManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		ChatManager.class,
		
		CommandManager.class,
		
		ServerHealthService.class,
		
		NetworkClientService.class,
		
		ZoneManager.class
})
public class GlobalManager extends Manager {
	
	public GlobalManager() {
		
	}
	
}
