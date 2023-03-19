package com.projectswg.holocore.services.gameplay;

import com.projectswg.holocore.services.gameplay.combat.CombatManager;
import com.projectswg.holocore.services.gameplay.conversation.ConversationService;
import com.projectswg.holocore.services.gameplay.crafting.CraftingManager;
import com.projectswg.holocore.services.gameplay.entertainment.EntertainmentManager;
import com.projectswg.holocore.services.gameplay.faction.FactionManager;
import com.projectswg.holocore.services.gameplay.jedi.JediManager;
import com.projectswg.holocore.services.gameplay.junkdealer.JunkDealerService;
import com.projectswg.holocore.services.gameplay.missions.DestroyMissionService;
import com.projectswg.holocore.services.gameplay.player.PlayerManager;
import com.projectswg.holocore.services.gameplay.structures.StructuresManager;
import com.projectswg.holocore.services.gameplay.world.WorldManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		CombatManager.class,
		ConversationService.class,
		CraftingManager.class,
		DestroyMissionService.class,
		EntertainmentManager.class,
		FactionManager.class,
		JediManager.class,
		JunkDealerService.class,
		PlayerManager.class,
		StructuresManager.class,
		WorldManager.class
})
public class GameplayManager extends Manager {
	
	public GameplayManager() {
		
	}
	
}
