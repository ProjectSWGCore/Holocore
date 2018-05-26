package com.projectswg.holocore.services.gameplay;

import com.projectswg.holocore.services.gameplay.combat.CombatManager;
import com.projectswg.holocore.services.gameplay.crafting.CraftingManager;
import com.projectswg.holocore.services.gameplay.entertainment.EntertainmentManager;
import com.projectswg.holocore.services.gameplay.gcw.GalacticCivilWarManager;
import com.projectswg.holocore.services.gameplay.player.PlayerManager;
import com.projectswg.holocore.services.gameplay.structures.StructuresManager;
import com.projectswg.holocore.services.gameplay.world.WorldManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		CombatManager.class,
		CraftingManager.class,
		EntertainmentManager.class,
		GalacticCivilWarManager.class,
		PlayerManager.class,
		StructuresManager.class,
		WorldManager.class
})
public class GameplayManager extends Manager {
	
	public GameplayManager() {
		
	}
	
}
