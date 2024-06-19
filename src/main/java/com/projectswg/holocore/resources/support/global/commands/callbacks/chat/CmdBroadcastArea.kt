package com.projectswg.holocore.resources.support.global.commands.callbacks.chat

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastArea
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class CmdBroadcastArea : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		broadcastArea(player, args)
	}
}
