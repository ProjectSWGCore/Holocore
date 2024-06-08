package com.projectswg.holocore.services.gameplay.jedi

import com.projectswg.holocore.intents.gameplay.player.experience.SkillModIntent
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.TimeUnit

class ForcePowerService : Service() {
	private val executor: ScheduledThreadPool = ScheduledThreadPool(1, "force-power-service")
	private val replenishDelay = TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS)

	override fun start(): Boolean {
		executor.start()
		executor.executeWithFixedRate(replenishDelay, replenishDelay) { this.periodicForceReplenish() }
		return super.start()
	}

	override fun stop(): Boolean {
		executor.stop()
		executor.awaitTermination(1000)
		return super.stop()
	}

	private fun periodicForceReplenish() {
		val loggedInCharacters = CharacterLookupService.PlayerLookup.getLoggedInCharacters()

		loggedInCharacters?.forEach {
			val playerObject = it.playerObject
			val forcePowerRegen = it.getSkillModValue("jedi_force_power_regen")
			val nextForcePower = Math.min(playerObject.forcePower + forcePowerRegen, playerObject.maxForcePower)

			playerObject.forcePower = nextForcePower
		}
	}

	@IntentHandler
	private fun handleSkillModIntent(intent: SkillModIntent) {
		val skillModName = intent.skillModName

		if (skillModName == "jedi_force_power_max") {
			intent.affectedCreatures.forEach {
				it.playerObject.maxForcePower += intent.adjustBase + intent.adjustModifier
			}
		}
	}
}