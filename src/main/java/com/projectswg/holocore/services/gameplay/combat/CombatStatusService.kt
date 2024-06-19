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
package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap

class CombatStatusService : Service() {
	
	private val inCombat: MutableSet<TangibleObject> = ConcurrentHashMap.newKeySet()
	private val executor: ScheduledThreadPool = ScheduledThreadPool(1, 3, "combat-status-service")

	override fun start(): Boolean {
		executor.start()
		executor.executeWithFixedRate(1000, 1000) { this.periodicCombatStatusChecks() }
		return true
	}
	
	override fun stop(): Boolean {
		executor.stop()
		executor.awaitTermination(1000)
		return true
	}
	
	private fun periodicCombatStatusChecks() {
		for (tangibleObject in inCombat) {
			if (tangibleObject.timeSinceLastCombat >= 10E3)
				ExitCombatIntent(tangibleObject).broadcast()
		}
	}
	
	@IntentHandler
	private fun handleEnterCombatIntent(eci: EnterCombatIntent) {
		val source = eci.source
		val target = eci.target
		if (source.isInCombat)
			return
		
		source.isInCombat = true
		target.addDefender(source)
		source.addDefender(target)
		inCombat.add(source)
		inCombat.add(target)
	}
	
	@IntentHandler
	private fun handleExitCombatIntent(eci: ExitCombatIntent) {
		val source = eci.source
		val defenders = source.defenders
			.filterNotNull()
			.mapNotNull { ObjectLookup.getObjectById(it) }
			.map { TangibleObject::class.java.cast(it) }
			.toList()
		source.clearDefenders()
		for (defender in defenders) {
			defender.removeDefender(source)
			if (!defender.hasDefenders())
				ExitCombatIntent(defender).broadcast()
		}
		if (source.hasDefenders())
			return
		
		source.isInCombat = false
		inCombat.remove(source)
	}

}
