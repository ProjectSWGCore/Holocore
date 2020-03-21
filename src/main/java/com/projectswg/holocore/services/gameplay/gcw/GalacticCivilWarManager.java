package com.projectswg.holocore.services.gameplay.gcw;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		CivilWarPointService.class,
		CivilWarPvpService.class,
		CivilWarRankService.class,
		CivilWarRegionService.class,
})
public class GalacticCivilWarManager extends Manager {
	
	public GalacticCivilWarManager() {
		
	}
	
}
