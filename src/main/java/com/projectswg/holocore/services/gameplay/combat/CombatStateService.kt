package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.gameplay.combat.ApplyCombatStateIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ScheduledFuture

class CombatStateService : Service() {

	private val stateDurationInMs = 10_000L
	private val executor: ScheduledThreadPool = ScheduledThreadPool(1, "combat-state-service")

	override fun start(): Boolean {
		executor.start()
		return super.start()
	}

	override fun stop(): Boolean {
		executor.stop()
		executor.awaitTermination(1000)
		return super.stop()
	}


	@IntentHandler
	private fun handleApplyCombatStateIntent(intent: ApplyCombatStateIntent) {
		val combatState = intent.combatState

		if (combatState == CombatState.BLINDED) {
			if (intent.victim.isStatesBitmask(CreatureState.BLINDED)) {
				return
			}

			val effectLoop = applyState(intent)

			executor.execute(stateDurationInMs) {
				clearState(intent, effectLoop)
			}
		}
	}

	private fun applyState(intent: ApplyCombatStateIntent): ScheduledFuture<*> {
		val blindEffectLabel = intent.combatState.effectLabel
		intent.victim.setStatesBitmask(CreatureState.BLINDED)

		val executeWithFixedRate = executor.executeWithFixedRate(0, 4_000) {
			if (intent.victim.isStatesBitmask(CreatureState.BLINDED)) {
				// For whatever reason, these effects only last a couple of seconds despite the state lasting longer. Repeat the effect.
				val stateBlindEffect = PlayClientEffectObjectMessage("appearance/pt_state_blind.prt", "rbrow2", intent.victim.objectId, blindEffectLabel)
				intent.victim.sendObservers(stateBlindEffect)
			}
		}

		val blindedEffect = PlayClientEffectObjectMessage("clienteffect/combat_special_defender_blind.cef", "", intent.victim.objectId, "")
		intent.victim.sendObservers(blindedEffect)

		val blindedFlytext = ShowFlyText(intent.victim.objectId, StringId("combat_effects", "go_blind"), ShowFlyText.Scale.MEDIUM, SWGColor.Greens.lawngreen)
		intent.victim.sendSelf(blindedFlytext)
		intent.attacker.sendSelf(blindedFlytext)

		val victimOwner = intent.victim.owner

		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@combat_effects:go_blind_single")
		}
		return executeWithFixedRate
	}

	private fun clearState(intent: ApplyCombatStateIntent, effectLoop: ScheduledFuture<*>) {
		val blindEffectLabel = intent.combatState.effectLabel
		intent.victim.clearStatesBitmask(CreatureState.BLINDED)

		val victimOwner = intent.victim.owner
		if (victimOwner != null) {
			SystemMessageIntent.broadcastPersonal(victimOwner, "@cbt_spam:no_blind_single")
		}

		val noBlindedFlytext = ShowFlyText(intent.victim.objectId, StringId("combat_effects", "no_blind"), ShowFlyText.Scale.MEDIUM, SWGColor.Reds.orangered)
		intent.victim.sendSelf(noBlindedFlytext)
		intent.attacker.sendSelf(noBlindedFlytext)

		stopRepeatingEffect(effectLoop, intent, blindEffectLabel)
	}

	private fun stopRepeatingEffect(effectLoop: ScheduledFuture<*>, intent: ApplyCombatStateIntent, blindEffectLabel: String) {
		effectLoop.cancel(false)
		val stopClientEffectObjectByLabelMessage = StopClientEffectObjectByLabelMessage(intent.victim.objectId, blindEffectLabel, false)
		intent.victim.sendObservers(stopClientEffectObjectByLabelMessage)
	}
}