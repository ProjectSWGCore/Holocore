package com.projectswg.holocore.resources.support.global.commands.callbacks.generic

import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class CmdSetMoodInternal : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		if (args.indexOf(' ') != -1) {
			return
		}

		player.creatureObject.moodId = args.toByte()
	}
}
