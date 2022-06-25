package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.intents.gameplay.combat.KnockdownIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class CombatKnockdownService : Service() {

	private val executor: ScheduledThreadPool = ScheduledThreadPool(1, 3, "combat-knockdown-service")

	override fun start(): Boolean {
		executor.start()
		return true
	}

	override fun stop(): Boolean {
		executor.stop()
		executor.awaitTermination(1000)
		return true
	}
	
	@IntentHandler
	private fun handleKnockdownIntent(intent: KnockdownIntent) {
		val victim = intent.victim

		victim.posture = Posture.KNOCKED_DOWN
		val owner = victim.owner
		
		if (owner != null) {
			SystemMessageIntent.broadcastPersonal(owner, ProsePackage("cbt_spam", "posture_knocked_down"))
		}

		val timeVictimShouldBeKnockedDown: Long = getTimeVictimShouldBeKnockedDown(victim)
		executor.execute(timeVictimShouldBeKnockedDown) {
			standUpIfStillKnockedDown(victim)
		}
	}

	private fun standUpIfStillKnockedDown(victim: CreatureObject) {
		if (victim.posture == Posture.KNOCKED_DOWN) {
			victim.posture = Posture.UPRIGHT
		}
	}

	private fun getTimeVictimShouldBeKnockedDown(victim: CreatureObject): Long {
		return if (victim.isPlayer) {
			30_000L
		} else {
			5_000L
		}
	}
}