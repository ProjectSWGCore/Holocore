package com.projectswg.holocore.resources.support.global.commands.callbacks.combat

import com.projectswg.holocore.intents.gameplay.combat.DuelPlayerIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class CmdDuel : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		if (target == null || target.owner == null) return

		val creoTarget = target as CreatureObject

		if (creoTarget.hasSentDuelRequestToPlayer(player.creatureObject)) {
			DuelPlayerIntent(player.creatureObject, creoTarget, DuelPlayerIntent.DuelEventType.ACCEPT).broadcast()
		} else {
			DuelPlayerIntent(player.creatureObject, creoTarget, DuelPlayerIntent.DuelEventType.REQUEST).broadcast()
		}
	}
}
