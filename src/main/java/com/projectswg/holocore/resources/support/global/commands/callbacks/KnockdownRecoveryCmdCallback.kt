package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.commands.Locomotion
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class KnockdownRecoveryCmdCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val creatureObject = player.creatureObject

		if (Locomotion.KNOCKED_DOWN.isActive(creatureObject)) {
			standUp(creatureObject)
		} else {
			showWrongPostureFlyText(creatureObject)
		}
	}

	private fun showWrongPostureFlyText(creatureObject: CreatureObject) {
		creatureObject.sendSelf(ShowFlyText(creatureObject.objectId, StringId("combat_effects", "wrong_posture_fly"), ShowFlyText.Scale.MEDIUM, SWGColor.Whites.white))
	}

	private fun standUp(creatureObject: CreatureObject) {
		creatureObject.posture = Posture.UPRIGHT
	}
}