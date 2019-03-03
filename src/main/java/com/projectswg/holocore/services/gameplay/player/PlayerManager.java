package com.projectswg.holocore.services.gameplay.player;

import com.projectswg.holocore.services.gameplay.player.badge.BadgeManager;
import com.projectswg.holocore.services.gameplay.player.collections.CollectionManager;
import com.projectswg.holocore.services.gameplay.player.equipment.EquipmentManager;
import com.projectswg.holocore.services.gameplay.player.experience.ExperienceManager;
import com.projectswg.holocore.services.gameplay.player.group.GroupManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		BadgeManager.class,
		CollectionManager.class,
		EquipmentManager.class,
		ExperienceManager.class,
		GroupManager.class
})
public class PlayerManager extends Manager {
	
	public PlayerManager() {
		
	}
	
}
