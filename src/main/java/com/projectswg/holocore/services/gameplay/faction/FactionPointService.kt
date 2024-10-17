/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.faction

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.utilities.ThreadUtilities
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class FactionPointService : Service() {

	private val executor = Executors.newSingleThreadExecutor(ThreadUtilities.newThreadFactory("faction-point-service-%d"))
	private val youHaveBeenAwardedPoints = StringId("base_player", "prose_award_faction")
	private val maxCapReached = StringId("base_player", "prose_max_faction")
	private val youHaveLostPoints = StringId("base_player", "prose_lose_faction")
	private val minCapReached = StringId("base_player", "prose_min_faction")

	override fun stop(): Boolean {
		executor.awaitTermination(1, TimeUnit.SECONDS)
		return super.stop()
	}

	@IntentHandler
	private fun handleCreatureKilled(creatureKilledIntent: CreatureKilledIntent) {
		val corpse = creatureKilledIntent.corpse
		val corpseFaction = corpse.faction ?: return
		val allies = corpseFaction.allies
		val enemies = corpseFaction.enemies
		val corpseFactionName = corpseFaction.name

		involvedPlayers(corpse).forEach { involvedPlayer ->
			executor.execute {
				decreaseFactionPoints(corpseFactionName, involvedPlayer)
				allies.forEach { allyFactionName -> decreaseFactionPoints(allyFactionName, involvedPlayer) }
				enemies.forEach { enemyFactionName -> increaseFactionPoints(enemyFactionName, involvedPlayer) }
			}
		}
	}

	private fun involvedPlayers(corpse: CreatureObject) = corpse.hateMap.keys.filter { it.isPlayer }.map { it.playerObject }

	private fun increaseFactionPoints(factionName: String, involvedPlayer: PlayerObject) {
		adjustFactionPoints(
			factionName = factionName,
			involvedPlayer = involvedPlayer,
			adjustment = 3,
			changeMessage = youHaveBeenAwardedPoints,
			capMessage = maxCapReached
		)
	}

	private fun decreaseFactionPoints(factionName: String, involvedPlayer: PlayerObject) {
		adjustFactionPoints(
			factionName = factionName,
			involvedPlayer = involvedPlayer,
			adjustment = -5,
			changeMessage = youHaveLostPoints,
			capMessage = minCapReached
		)
	}

	private fun adjustFactionPoints(factionName: String, involvedPlayer: PlayerObject, adjustment: Int, changeMessage: StringId, capMessage: StringId) {
		val owner = involvedPlayer.owner ?: return
		val delta = involvedPlayer.adjustFactionPoints(factionName, adjustment)

		if (delta != 0) {
			val prosePackage = ProsePackage(changeMessage, "DI", abs(delta), "TO", "@faction/faction_names:$factionName")
			SystemMessageIntent.broadcastPersonal(owner, prosePackage)
		} else {
			val prosePackage = ProsePackage(capMessage, "TO", "@faction/faction_names:$factionName")
			SystemMessageIntent.broadcastPersonal(owner, prosePackage)
		}
	}
}