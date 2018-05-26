package com.projectswg.holocore.services.support.data;

import com.projectswg.holocore.services.support.data.dev.DeveloperService;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		DeveloperService.class,
		
		PacketRecordingService.class,
		ServerDataService.class,
		ServerStatusService.class
})
public class SupportDataManager extends Manager {
	
	public SupportDataManager() {
		
	}
	
}
