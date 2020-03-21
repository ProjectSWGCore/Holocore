package com.projectswg.holocore.services.gameplay.player.equipment;

import com.projectswg.holocore.services.gameplay.player.experience.skills.ProtectionService;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		ProtectionService.class,
})
public class EquipmentManager extends Manager {
	public EquipmentManager() {
	}
}
