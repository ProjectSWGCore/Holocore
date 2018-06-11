package com.projectswg.holocore.resources.support.global.commands.callbacks.generic;

import com.projectswg.holocore.intents.gameplay.entertainment.dance.DanceIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

public final class CmdStopDance implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		new DanceIntent(player.getCreatureObject()).broadcast();
	}
	
}
