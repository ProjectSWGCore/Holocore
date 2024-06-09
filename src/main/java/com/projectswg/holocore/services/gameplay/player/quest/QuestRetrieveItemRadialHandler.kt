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
package com.projectswg.holocore.services.gameplay.player.quest

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.intents.gameplay.player.quest.QuestRetrieveItemIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.QuestLoader.QuestTaskInfo
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class QuestRetrieveItemRadialHandler(private val retrievedItemRepository: RetrievedItemRepository, private val questName: String, private val task: QuestTaskInfo) : RadialHandlerInterface {
	private val radialItem = RadialItem.SERVER_MENU1
	private val retriveItemInfo = task.retrieveItemInfo!!  // Precondition for this class

	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		val playerObject = player.playerObject
		val questActiveTasks = playerObject.getQuestActiveTasks(questName)
		
		if (questActiveTasks.contains(task.index)) {
			if (target.template.equals(retriveItemInfo.serverTemplate)) {
				if (!retrievedItemRepository.hasAttemptedPreviously(questName, playerObject, target)) {
					options.add(RadialOption.create(radialItem, retriveItemInfo.retrieveMenuText))
				}
			}
		}
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		if (selection != radialItem)
			return
		
		QuestRetrieveItemIntent(player, questName, task, target).broadcast()
	}
}
