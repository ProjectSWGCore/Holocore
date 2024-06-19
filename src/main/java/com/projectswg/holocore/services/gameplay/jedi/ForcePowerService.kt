/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.gameplay.jedi

import com.projectswg.holocore.intents.gameplay.player.experience.SkillModIntent
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.TimeUnit
import kotlin.math.min

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
			val nextForcePower = min(playerObject.forcePower + forcePowerRegen, playerObject.maxForcePower)

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