package com.projectswg.holocore.resources.support.global.commands.callbacks.chat;

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

public final class CmdBroadcastPlanet implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null)
			return;
		SystemMessageIntent.broadcastPlanet(creature.getTerrain(), args);
	}
	
}
