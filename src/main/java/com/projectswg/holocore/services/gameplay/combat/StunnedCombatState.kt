package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

class StunnedCombatState : CombatState {

	private val loopEffectLabel = "stun"

	override fun isApplied(victim: CreatureObject): Boolean {
		return victim.isStatesBitmask(CreatureState.STUNNED)
	}

	override fun apply(attacker: CreatureObject, victim: CreatureObject) {
		victim.setStatesBitmask(CreatureState.STUNNED)

		val stunnedEffect = PlayClientEffectObjectMessage("clienteffect/combat_special_defender_stun.cef", "", victim.objectId, "")
		victim.sendObservers(stunnedEffect)

		val stunnedFlytext = ShowFlyText(victim.objectId, StringId("combat_effects", "go_stunned"), ShowFlyText.Scale.MEDIUM, SWGColor.Greens.lawngreen)
		victim.sendSelf(stunnedFlytext)
		attacker.sendSelf(stunnedFlytext)

		val victimOwner = victim.owner

		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@cbt_spam:go_stunned_single")
		}
	}

	override fun loop(attacker: CreatureObject, victim: CreatureObject) {
		val stateStunEffect = PlayClientEffectObjectMessage("appearance/pt_state_stunned.prt", "rbrow2", victim.objectId, loopEffectLabel)
		victim.sendObservers(stateStunEffect)
	}

	override fun clear(attacker: CreatureObject, victim: CreatureObject) {
		victim.clearStatesBitmask(CreatureState.STUNNED)

		val victimOwner = victim.owner
		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@cbt_spam:no_stunned_single")
		}

		val noStunnedFlytext = ShowFlyText(victim.objectId, StringId("combat_effects", "no_stunned"), ShowFlyText.Scale.MEDIUM, SWGColor.Reds.orangered)
		victim.sendSelf(noStunnedFlytext)
		attacker.sendSelf(noStunnedFlytext)

		val stopClientEffectObjectByLabelMessage = StopClientEffectObjectByLabelMessage(victim.objectId, loopEffectLabel, false)
		victim.sendObservers(stopClientEffectObjectByLabelMessage)
	}

}