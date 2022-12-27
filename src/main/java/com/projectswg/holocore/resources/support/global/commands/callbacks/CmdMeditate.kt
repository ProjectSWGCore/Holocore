package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class CmdMeditate : ICmdCallback {
    override fun execute(player: Player, target: SWGObject?, args: String) {
        player.creatureObject.moodAnimation = "meditating"
    }
}