package com.projectswg.holocore.resources.support.global.commands.callbacks.chat;

import com.projectswg.holocore.intents.support.global.chat.SpatialChatIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

public final class CmdSpatialChatInternal implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		String[] cmdArgs = args.split(" ", 5);
		
		new SpatialChatIntent(player, Integer.valueOf(cmdArgs[1]), args.substring(10), Integer.valueOf(cmdArgs[2])).broadcast();
	}
	
}
