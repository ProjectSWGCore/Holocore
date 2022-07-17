package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.combat.CombatSpamType
import com.projectswg.common.data.encodables.oob.OutOfBandPackage
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam
import com.projectswg.holocore.intents.gameplay.combat.RequestCreatureDeathIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

class BleedingCombatState : CombatState {

	private val loopEffectLabel = "bleed"

	override fun isApplied(victim: CreatureObject): Boolean {
		return victim.isStatesBitmask(CreatureState.BLEEDING)
	}

	override fun apply(attacker: CreatureObject, victim: CreatureObject) {
		victim.setStatesBitmask(CreatureState.BLEEDING)

		val dotBleedingEffect = PlayClientEffectObjectMessage("clienteffect/dot_bleeding.cef", "", victim.objectId, "")
		victim.sendObservers(dotBleedingEffect)

		val victimOwner = victim.owner
		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@dot_message:start_bleeding")
		}
	}

	override fun loop(attacker: CreatureObject, victim: CreatureObject) {
		val dotBleedingEffect = PlayClientEffectObjectMessage("appearance/pt_state_bleeding.prt", "root", victim.objectId, loopEffectLabel)
		victim.sendObservers(dotBleedingEffect)

		val damage = 75
		if (damage > victim.health) {
			RequestCreatureDeathIntent.broadcast(attacker, victim)
		} else {
			victim.modifyHealth(-damage)
			val victimObservers = victim.observerCreatures
			val spamMessage = OutOfBandPackage(ProsePackage(StringId("dot_message", "bleed_dmg_atkr"), "TO", attacker.objectName, "TT", victim.objectName, "DI", damage))

			victimObservers.forEach { observer ->
				val combatSpam = CombatSpam(observer.objectId)
				combatSpam.spamType = CombatSpamType.HIT
				combatSpam.defender = victim.objectId
				combatSpam.attacker = attacker.objectId
				combatSpam.weapon = attacker.equippedWeapon.objectId
				combatSpam.dataType = 2
				combatSpam.spamMessage = spamMessage

				observer.sendSelf(combatSpam)
			}
		}
	}

	override fun clear(attacker: CreatureObject, victim: CreatureObject) {
		victim.clearStatesBitmask(CreatureState.BLEEDING)

		val stopClientEffectObjectByLabelMessage = StopClientEffectObjectByLabelMessage(victim.objectId, loopEffectLabel, false)
		victim.sendObservers(stopClientEffectObjectByLabelMessage)

		val victimOwner = victim.owner
		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@dot_message:stop_bleeding")
		}
	}

}