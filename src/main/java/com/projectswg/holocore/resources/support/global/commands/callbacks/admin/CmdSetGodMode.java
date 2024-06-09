package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.awareness.ForceAwarenessUpdateIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

public final class CmdSetGodMode implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		CreatureObject creatureObject = player.getCreatureObject();
		
		if (creatureObject.hasCommand("admin")) {
			creatureObject.removeCommand("admin");
			SystemMessageIntent.Companion.broadcastPersonal(player, "God Mode Disabled");
		} else {
			creatureObject.addCommand("admin");
			SystemMessageIntent.Companion.broadcastPersonal(player, "God Mode Enabled");
		}
		ForceAwarenessUpdateIntent.broadcast(creatureObject);
	}
	
}
