package com.projectswg.holocore.resources.support.global.commands.callbacks.group;

import com.projectswg.holocore.intents.gameplay.player.group.GroupEventUninvite;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

public final class CmdGroupUninvite implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		if (target instanceof CreatureObject creatureTarget) {
			new GroupEventUninvite(player, creatureTarget).broadcast();
		} else {
			SystemMessageIntent.broadcastPersonal(player, "@group:uninvite_no_target_self");
		}
	}
	
}
