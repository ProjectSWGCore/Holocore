package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

public final class CmdSetGodMode implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		if (player.getAccessLevel() == AccessLevel.PLAYER) {
			SystemMessageIntent.broadcastPersonal(player, "Players cannot use this command :(");
			return;
		}
		
		CreatureObject creatureObject = player.getCreatureObject();
		
		if (creatureObject.hasAbility("admin")) {
			creatureObject.removeAbility("admin");
			SystemMessageIntent.broadcastPersonal(player, "God Mode Disabled");
		} else {
			creatureObject.addAbility("admin");
			SystemMessageIntent.broadcastPersonal(player, "God Mode Enabled");
		}
	}
	
}
