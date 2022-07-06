package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.MovementModifierIdentifier

class BurstRunCmdCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val creatureObject = player.creatureObject
		creatureObject.setMovementScale(MovementModifierIdentifier.BURST_RUN, 2f, false)
	}
}