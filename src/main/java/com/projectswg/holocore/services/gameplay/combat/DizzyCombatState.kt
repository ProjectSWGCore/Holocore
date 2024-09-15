package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

class DizzyCombatState : CombatState {

	private val loopEffectLabel = "dizzy"

	override fun isApplied(victim: CreatureObject): Boolean {
		return victim.isStatesBitmask(CreatureState.DIZZY)
	}

	override fun apply(attacker: CreatureObject, victim: CreatureObject) {
		victim.setStatesBitmask(CreatureState.DIZZY)

		val dizzyEffect = PlayClientEffectObjectMessage("clienteffect/combat_special_defender_dizzy.cef", "", victim.objectId, "")
		victim.sendObservers(dizzyEffect)

		val dizzyFlyText = ShowFlyText(victim.objectId, StringId("combat_effects", "go_dizzy"), ShowFlyText.Scale.MEDIUM, SWGColor.Greens.lawngreen)
		victim.sendSelf(dizzyFlyText)
		attacker.sendSelf(dizzyFlyText)

		val victimOwner = victim.owner

		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@cbt_spam:go_dizzy_single")
		}
	}

	override fun loop(attacker: CreatureObject, victim: CreatureObject) {
		val stateDizzyEffect = PlayClientEffectObjectMessage("appearance/pt_dizzy_player.prt", "rbrow2", victim.objectId, loopEffectLabel)
		victim.sendObservers(stateDizzyEffect)
	}

	override fun clear(attacker: CreatureObject, victim: CreatureObject) {
		victim.clearStatesBitmask(CreatureState.DIZZY)

		val victimOwner = victim.owner
		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@cbt_spam:no_dizzy_single")
		}

		val noDizzyFlyText = ShowFlyText(victim.objectId, StringId("combat_effects", "no_dizzy"), ShowFlyText.Scale.MEDIUM, SWGColor.Reds.orangered)
		victim.sendSelf(noDizzyFlyText)
		attacker.sendSelf(noDizzyFlyText)

		val stopClientEffectObjectByLabelMessage = StopClientEffectObjectByLabelMessage(victim.objectId, loopEffectLabel, false)
		victim.sendObservers(stopClientEffectObjectByLabelMessage)
	}

}