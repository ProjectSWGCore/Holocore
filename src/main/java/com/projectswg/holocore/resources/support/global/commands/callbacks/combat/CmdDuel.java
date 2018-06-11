package com.projectswg.holocore.resources.support.global.commands.callbacks.combat;

import com.projectswg.holocore.intents.gameplay.combat.duel.DuelPlayerIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

public final class CmdDuel implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		if (target == null || target.getOwner() == null)
			return;
		
		CreatureObject creoTarget = (CreatureObject) target;
		
		if (creoTarget.hasSentDuelRequestToPlayer(player.getCreatureObject())) {
			new DuelPlayerIntent(player.getCreatureObject(), creoTarget, DuelPlayerIntent.DuelEventType.ACCEPT).broadcast();
		} else {
			new DuelPlayerIntent(player.getCreatureObject(), creoTarget, DuelPlayerIntent.DuelEventType.REQUEST).broadcast();
		}
	}
	
}
