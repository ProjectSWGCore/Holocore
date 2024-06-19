package com.projectswg.holocore.resources.support.global.commands.callbacks.admin

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.MovementModifierIdentifier

class CmdSetSpeed : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val creature = player.creatureObject

		try {
			creature.setMovementScale(MovementModifierIdentifier.SET_SPEED, args.toInt().toFloat(), false)
		} catch (e: NumberFormatException) {
			broadcastPersonal(player, "$args is not a valid number!")
		}
	}
}
