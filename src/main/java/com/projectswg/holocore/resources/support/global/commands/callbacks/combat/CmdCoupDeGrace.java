package com.projectswg.holocore.resources.support.global.commands.callbacks.combat;

import com.projectswg.holocore.intents.gameplay.combat.DeathblowIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

public final class CmdCoupDeGrace implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		new DeathblowIntent(player.getCreatureObject(), (CreatureObject) target).broadcast();
	}
	
}
