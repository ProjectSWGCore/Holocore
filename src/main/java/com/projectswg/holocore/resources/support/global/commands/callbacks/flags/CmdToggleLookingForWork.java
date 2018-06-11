package com.projectswg.holocore.resources.support.global.commands.callbacks.flags;

import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

public final class CmdToggleLookingForWork implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		player.getPlayerObject().toggleFlag(PlayerFlags.LFW);
	}
	
}
