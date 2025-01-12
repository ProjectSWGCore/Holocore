/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.support.npc.ai

import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap

class AIService : Service() {
	private val coroutineScope = CoroutineScope(context = SupervisorJob() + Dispatchers.Default)
	private val aiObjects: MutableCollection<AIObject> = ConcurrentHashMap.newKeySet()

	override fun start(): Boolean {
		for (obj in aiObjects) {
			obj.start(coroutineScope)
		}
		return true
	}

	override fun stop(): Boolean {
		coroutineScope.cancel()
		aiObjects.clear()
		return super.stop()
	}

	@IntentHandler
	private fun handleObjectCreatedIntent(oci: ObjectCreatedIntent) {
		val obj = oci.obj as? AIObject ?: return
		if (aiObjects.add(obj)) obj.start(coroutineScope)
	}

	@IntentHandler
	private fun handleDestroyObjectIntent(doi: DestroyObjectIntent) {
		val obj = doi.obj as? AIObject ?: return
		if (aiObjects.remove(obj)) obj.stop()
	}

	@IntentHandler
	private fun handleCreatureKilledIntent(cki: CreatureKilledIntent) {
		val corpse = cki.corpse
		if (corpse is AIObject)
			corpse.stop()
	}

	@IntentHandler
	private fun handleEnterCombatIntent(eci: EnterCombatIntent) {
		val obj = eci.source as? AIObject ?: return
		val target = eci.target as? CreatureObject ?: return
		obj.startCombat(listOf(target))
	}
}
