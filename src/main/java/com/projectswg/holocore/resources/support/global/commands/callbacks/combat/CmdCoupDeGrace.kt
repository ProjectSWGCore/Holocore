package com.projectswg.holocore.resources.support.global.commands.callbacks.combat

import com.projectswg.holocore.intents.gameplay.combat.DeathblowIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class CmdCoupDeGrace : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		DeathblowIntent(player.creatureObject, (target as CreatureObject?)!!).broadcast()
	}
}
