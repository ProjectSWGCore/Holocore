package com.projectswg.holocore.resources.support.global.commands.callbacks.chat;

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

public final class CmdBroadcastArea implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		SystemMessageIntent.broadcastArea(player, args);
	}
	
}
