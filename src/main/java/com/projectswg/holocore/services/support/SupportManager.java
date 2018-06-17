package com.projectswg.holocore.services.support;

import com.projectswg.holocore.services.support.data.SupportDataManager;
import com.projectswg.holocore.services.support.global.GlobalManager;
import com.projectswg.holocore.services.support.npc.NonPlayerCharacterManager;
import com.projectswg.holocore.services.support.objects.ObjectManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		SupportDataManager.class,
		ObjectManager.class,
		GlobalManager.class,
		NonPlayerCharacterManager.class
})
public class SupportManager extends Manager {
	
	public SupportManager() {
		
	}
	
}
