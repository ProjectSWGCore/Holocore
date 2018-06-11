package com.projectswg.holocore.resources.support.global.commands.callbacks.group;

import com.projectswg.holocore.intents.gameplay.player.group.GroupEventIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

public final class CmdGroupDisband implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_DISBAND, player, null).broadcast();
	}
	
}
