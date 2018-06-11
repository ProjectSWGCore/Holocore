package com.projectswg.holocore.resources.support.global.commands.callbacks.combat;

import com.projectswg.holocore.intents.gameplay.combat.duel.DuelPlayerIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

public final class CmdEndDuel implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		if (!(target instanceof CreatureObject))
			return;
		
		if (player.getCreatureObject().hasSentDuelRequestToPlayer((CreatureObject) target) && !player.getCreatureObject().isDuelingPlayer((CreatureObject) target)) {
			new DuelPlayerIntent(player.getCreatureObject(), (CreatureObject) target, DuelPlayerIntent.DuelEventType.CANCEL).broadcast();
		} else if (((CreatureObject) target).hasSentDuelRequestToPlayer(player.getCreatureObject()) && !player.getCreatureObject().hasSentDuelRequestToPlayer((CreatureObject) target)) {
			new DuelPlayerIntent(player.getCreatureObject(), (CreatureObject) target, DuelPlayerIntent.DuelEventType.DECLINE).broadcast();
		} else {
			new DuelPlayerIntent(player.getCreatureObject(), (CreatureObject) target, DuelPlayerIntent.DuelEventType.END).broadcast();
		}
	}
	
}
