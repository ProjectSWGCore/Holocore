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
package com.projectswg.holocore.resources.support.objects.radial.`object`

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.intents.gameplay.entertainment.WatchIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject

class CreatureObjectRadial : RadialHandlerInterface {
	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		if (target !is CreatureObject) return
		
		watch(options, player, target)
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		when (selection) {
			RadialItem.SERVER_PERFORMANCE_WATCH      -> WatchIntent(player, target, true).broadcast()
			RadialItem.SERVER_PERFORMANCE_WATCH_STOP -> WatchIntent(player, target, false).broadcast()
			else                                     -> {}
		}
	}

	private fun watch(options: MutableCollection<RadialOption>, player: Player, target: CreatureObject) {
		if (!target.isPlayer) return
		if (!target.isPerforming) return
		val dancing = target.performanceId == 0
		if (!dancing) return
		
		val currentlyWatching = player.creatureObject.performanceListenTarget == target.objectId

		if (currentlyWatching) {
			options.add(RadialOption.create(RadialItem.SERVER_PERFORMANCE_WATCH_STOP, "@radial_performance:watch_stop"))
		} else {
			options.add(RadialOption.create(RadialItem.SERVER_PERFORMANCE_WATCH, "@radial_performance:watch"))
		}
	}
}