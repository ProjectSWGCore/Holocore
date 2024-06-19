package com.projectswg.holocore.resources.support.global.commands.callbacks.admin

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class CmdMoney : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val argSplit = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		if (argSplit.size < 2) {
			broadcastPersonal(player, "Invalid Arguments: $args")
			return
		}
		val creature = player.creatureObject
		when (argSplit[0]) {
			"bank" -> creature.setBankBalance(creature.bankBalance + argSplit[1].toLong())
			"cash" -> creature.setCashBalance(creature.cashBalance + argSplit[1].toLong())
			else   -> broadcastPersonal(player, "Unknown Destination: " + argSplit[0])
		}
	}
}
