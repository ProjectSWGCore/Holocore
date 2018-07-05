package com.projectswg.holocore.resources.support.global.commands.callbacks.group;

import com.projectswg.holocore.intents.gameplay.player.group.GroupEventIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

public final class CmdGroupDecline implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_DECLINE, player).broadcast();
	}
	
}
