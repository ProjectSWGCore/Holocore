/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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

import com.projectswg.holocore.intents.gameplay.combat.CorpseLootedIntent
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.gameplay.combat.LootLotteryStartedIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max

class CombatNpcService : Service() {
	private val deleteCorpseTasks = ConcurrentHashMap<Long, DeleteCorpseLaterJob>()
	private val coroutineScope = HolocoreCoroutine.childScope()

	override fun stop(): Boolean {
		coroutineScope.cancelAndWait()
		return super.stop()
	}

	@IntentHandler
	private fun handleCreatureKilledIntent(i: CreatureKilledIntent) {
		val corpse = i.corpse
		if (corpse.isPlayer) return

		if (i.killer.isPlayer) {
			deleteCorpseTasks[corpse.objectId] = DeleteCorpseLaterJob(120, corpse)
		} else {
			DeleteCorpseLaterJob(60, corpse)
		}
	}

	@IntentHandler
	private fun handleLootLotteryStartedIntent(llsi: LootLotteryStartedIntent) {
		val corpse = llsi.corpse
		assert(!corpse.isPlayer) { "Cannot (shouldn't) loot a player" }

		val task = deleteCorpseTasks[corpse.objectId] ?: return
		if (task.getDelaySeconds() < 35) {
			deleteCorpseTasks.put(corpse.objectId, DeleteCorpseLaterJob(35, corpse))?.cancel()
		}
	}

	@IntentHandler
	private fun handleCorpseLootedIntent(cli: CorpseLootedIntent) {
		val corpse = cli.corpse
		assert(!corpse.isPlayer) { "Cannot (shouldn't) loot a player" }

		val task = deleteCorpseTasks[corpse.objectId]

		if (task == null) {
			Log.w("There should already be a deleteCorpse task for corpse %s!", corpse.toString())
			DeleteCorpseLaterJob(30, corpse)
			return
		}
		if (task.getDelaySeconds() > 15) {
			deleteCorpseTasks.put(corpse.objectId, DeleteCorpseLaterJob(30, corpse))?.cancel()
		}
	}

	private fun deleteCorpse(creatureCorpse: CreatureObject) {
		DestroyObjectIntent(creatureCorpse).broadcast()
		deleteCorpseTasks.remove(creatureCorpse.objectId)
		Log.d("Corpse of NPC %s was deleted from the world", creatureCorpse)
	}
	
	private inner class DeleteCorpseLaterJob(delaySeconds: Long, corpse: CreatureObject) {
		
		private val deletesAfter = System.nanoTime() + TimeUnit.SECONDS.toNanos(delaySeconds)
		private val job = coroutineScope.launch {
			delay(TimeUnit.SECONDS.toMillis(delaySeconds))
			deleteCorpse(corpse)
		}
		
		fun getDelaySeconds(): Long {
			return max(0L, TimeUnit.NANOSECONDS.toMillis(deletesAfter - System.nanoTime()))
		}
		
		fun cancel() {
			job.cancel()
		}
		
	}
}