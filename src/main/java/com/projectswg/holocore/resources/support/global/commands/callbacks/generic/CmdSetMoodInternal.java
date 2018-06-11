package com.projectswg.holocore.resources.support.global.commands.callbacks.generic;

import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

public final class CmdSetMoodInternal implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		if (args.indexOf(' ') != -1) {
			return;
		}
		
		player.getCreatureObject().setMoodId(Byte.valueOf(args));
	}
	
}
