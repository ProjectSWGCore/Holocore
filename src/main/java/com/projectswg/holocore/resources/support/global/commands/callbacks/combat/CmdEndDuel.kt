package com.projectswg.holocore.resources.support.global.commands.callbacks.combat

import com.projectswg.holocore.intents.gameplay.combat.DuelPlayerIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class CmdEndDuel : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		if (target !is CreatureObject) return

		if (player.creatureObject.hasSentDuelRequestToPlayer(target as CreatureObject?) && !player.creatureObject.isDuelingPlayer(target as CreatureObject?)) {
			DuelPlayerIntent(player.creatureObject, (target as CreatureObject?)!!, DuelPlayerIntent.DuelEventType.CANCEL).broadcast()
		} else if (target.hasSentDuelRequestToPlayer(player.creatureObject) && !player.creatureObject.hasSentDuelRequestToPlayer(target as CreatureObject?)) {
			DuelPlayerIntent(player.creatureObject, (target as CreatureObject?)!!, DuelPlayerIntent.DuelEventType.DECLINE).broadcast()
		} else {
			DuelPlayerIntent(player.creatureObject, (target as CreatureObject?)!!, DuelPlayerIntent.DuelEventType.END).broadcast()
		}
	}
}
