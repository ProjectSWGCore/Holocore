package com.projectswg.holocore.resources.support.global.commands.callbacks.generic;

import com.projectswg.holocore.intents.gameplay.entertainment.dance.WatchIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

public final class CmdWatch implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		if (target == null) {
			return;
		}
		
		if (player.getCreatureObject() == target) {
			// You can't watch yourself, silly!
			return;
		}
		
		new WatchIntent(player.getCreatureObject(), target, true).broadcast();
	}
	
}
