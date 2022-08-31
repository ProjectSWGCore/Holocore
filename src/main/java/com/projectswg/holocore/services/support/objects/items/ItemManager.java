package com.projectswg.holocore.services.support.objects.items;

import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		StaticItemService.class,
		ContainerService.class,
})
public class ItemManager extends Manager {
	
	public ItemManager() {
		
	}
	
}
