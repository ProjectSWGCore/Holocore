package com.projectswg.holocore.resources.support.global.commands.callbacks.group;

import com.projectswg.holocore.intents.gameplay.player.group.GroupEventIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

public final class CmdGroupKick implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		if (!(target instanceof CreatureObject))
			return;
		new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_KICK, player, (CreatureObject) target).broadcast();
	}
	
}
