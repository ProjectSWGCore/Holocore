package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

enum class Locomotion(val locomotionTableId: Int, val commandSdbColumnName: String, val check: (CreatureObject) -> Boolean) {
	KNOCKED_DOWN(18, "L:knockedDown", {creatureObject -> creatureObject.posture == com.projectswg.common.data.encodables.tangible.Posture.KNOCKED_DOWN})
}