package com.projectswg.holocore.services.gameplay.gcw;

import com.projectswg.holocore.services.gameplay.gcw.faction.FactionManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		FactionManager.class
})
public class GalacticCivilWarManager extends Manager {
	
	public GalacticCivilWarManager() {
		
	}
	
}
