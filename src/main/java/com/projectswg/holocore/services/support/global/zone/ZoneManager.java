package com.projectswg.holocore.services.support.global.zone;

import com.projectswg.holocore.services.support.global.zone.creation.CharacterCreationService;
import com.projectswg.holocore.services.support.global.zone.sui.SuiService;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		CharacterCreationService.class,
		
		SuiService.class,
		
		CharacterLookupService.class,
		ConnectionService.class,
		LoginService.class,
		PlayerSessionService.class,
		ZoneService.class
})
public class ZoneManager extends Manager {
	
	public ZoneManager() {
		
	}
	
}
