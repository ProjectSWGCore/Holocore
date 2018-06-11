package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

public final class CmdSetSpeed implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		CreatureObject creature = player.getCreatureObject();
		
		try {
			creature.setMovementScale(Integer.valueOf(args));
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(player, args + " is not a valid number!");
		}
	}
	
}
