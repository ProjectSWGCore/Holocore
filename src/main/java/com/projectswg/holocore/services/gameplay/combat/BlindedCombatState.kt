package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

class BlindedCombatState : CombatState {

	private val loopEffectLabel = "blind"

	override fun isApplied(victim: CreatureObject): Boolean {
		return victim.isStatesBitmask(CreatureState.BLINDED)
	}

	override fun apply(attacker: CreatureObject, victim: CreatureObject) {
		victim.setStatesBitmask(CreatureState.BLINDED)

		val blindedEffect = PlayClientEffectObjectMessage("clienteffect/combat_special_defender_blind.cef", "", victim.objectId, "")
		victim.sendObservers(blindedEffect)

		val blindedFlytext = ShowFlyText(victim.objectId, StringId("combat_effects", "go_blind"), ShowFlyText.Scale.MEDIUM, SWGColor.Greens.lawngreen)
		victim.sendSelf(blindedFlytext)
		attacker.sendSelf(blindedFlytext)

		val victimOwner = victim.owner

		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@combat_effects:go_blind_single")
		}
	}

	override fun loop(attacker: CreatureObject, victim: CreatureObject) {
		val stateBlindEffect = PlayClientEffectObjectMessage("appearance/pt_state_blind.prt", "rbrow2", victim.objectId, loopEffectLabel)
		victim.sendObservers(stateBlindEffect)
	}

	override fun clear(attacker: CreatureObject, victim: CreatureObject) {
		victim.clearStatesBitmask(CreatureState.BLINDED)

		val victimOwner = victim.owner
		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@cbt_spam:no_blind_single")
		}

		val noBlindedFlytext = ShowFlyText(victim.objectId, StringId("combat_effects", "no_blind"), ShowFlyText.Scale.MEDIUM, SWGColor.Reds.orangered)
		victim.sendSelf(noBlindedFlytext)
		attacker.sendSelf(noBlindedFlytext)

		val stopClientEffectObjectByLabelMessage = StopClientEffectObjectByLabelMessage(victim.objectId, loopEffectLabel, false)
		victim.sendObservers(stopClientEffectObjectByLabelMessage)
	}

}