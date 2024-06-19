package com.projectswg.holocore.resources.support.global.commands.callbacks.chat

import com.projectswg.holocore.intents.support.global.chat.SpatialChatIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class CmdSpatialChatInternal : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val cmdArgs = args.split(" ".toRegex(), limit = 6).toTypedArray()

		SpatialChatIntent(player, cmdArgs[1].toInt(), args.substring(10), cmdArgs[2].toInt(), cmdArgs[4].toInt()).broadcast()
	}
}
