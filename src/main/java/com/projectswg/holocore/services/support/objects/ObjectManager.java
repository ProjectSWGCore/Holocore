package com.projectswg.holocore.services.support.objects;

import com.projectswg.holocore.services.support.objects.awareness.AwarenessService;
import com.projectswg.holocore.services.support.objects.buildouts.StaticService;
import com.projectswg.holocore.services.support.objects.items.ItemManager;
import com.projectswg.holocore.services.support.objects.radials.RadialService;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		AwarenessService.class,
		
		StaticService.class,
		
		ItemManager.class,
		
		RadialService.class,
		
		ObjectStorageService.class
})
public class ObjectManager extends Manager {
	
	public ObjectManager() {
		
	}
	
}
