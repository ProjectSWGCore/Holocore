package com.projectswg.holocore.services.gameplay.structures;

import com.projectswg.holocore.services.gameplay.structures.housing.HousingManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		HousingManager.class
})
public class StructuresManager extends Manager {
	
	public StructuresManager() {
		
	}
	
}
