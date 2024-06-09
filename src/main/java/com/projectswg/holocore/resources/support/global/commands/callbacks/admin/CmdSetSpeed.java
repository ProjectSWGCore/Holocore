package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.MovementModifierIdentifier;
import org.jetbrains.annotations.NotNull;

public final class CmdSetSpeed implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		CreatureObject creature = player.getCreatureObject();
		
		try {
			creature.setMovementScale(MovementModifierIdentifier.SET_SPEED, Integer.parseInt(args), false);
		} catch (NumberFormatException e) {
			SystemMessageIntent.Companion.broadcastPersonal(player, args + " is not a valid number!");
		}
	}
	
}
