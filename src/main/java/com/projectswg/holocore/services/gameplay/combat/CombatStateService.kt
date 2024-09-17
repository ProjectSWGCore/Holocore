package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.holocore.intents.gameplay.combat.ApplyCombatStateIntent
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class CombatStateService : Service() {
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
		val victim = intent.victim
		val attacker = intent.attacker
		val cancelState = intent.cancelState
		val runTime: Long = (intent.duration * 1000).toLong()

		if (!combatState.isApplied(victim)) {
			combatState.apply(attacker!!, victim)

			val loop = executor.executeWithFixedRate(0, 4_000) {
				if (combatState.isApplied(victim)) {
					combatState.loop(attacker, victim)
				}
			}

			executor.execute(runTime) {
				loop.cancel(false)
				combatState.clear(attacker, victim)
			}
		}
		if (cancelState) {
			combatState.clear(attacker, victim)
		}
	}
}