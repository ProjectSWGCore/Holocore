package com.projectswg.holocore.resources.gameplay.conversation.events;

import com.projectswg.holocore.resources.gameplay.conversation.model.Event;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.services.gameplay.training.*;

public class ShowAvailableSkillsEvent implements Event {
	
	private final String professionName;
	
	public ShowAvailableSkillsEvent(String professionName) {
		this.professionName = professionName;
	}
	
	@Override
	public void trigger(Player player, AIObject npc) {
		AvailableSkillsWindow availableSkillsWindow = new AvailableSkillsWindow();
		
		availableSkillsWindow.show(professionName, player);
	}
}
