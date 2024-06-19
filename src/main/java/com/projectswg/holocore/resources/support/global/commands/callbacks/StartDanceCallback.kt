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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.gameplay.entertainment.StartDanceIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

open class StartDanceCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		handleCommand(player, target, args, false)
	}

	protected fun handleCommand(player: Player, target: SWGObject?, args: String, changeDance: Boolean) {
		val creatureObject = player.creatureObject

		// Not sure if args can ever actually be null. Better safe than sorry.
		if (args.isEmpty()) {
			// If no args are given, then bring up the SUI window for dance selection.
			val abilityNames = creatureObject.commands
			SuiListBox().run {
				title = "@performance:select_dance"
				prompt = "@performance:available_dances"

				for (abilityName in abilityNames) {
					if (abilityName.startsWith(ABILITY_NAME_PREFIX)) {
						val displayName = abilityName.replace(ABILITY_NAME_PREFIX, "")
						val firstCharacter = displayName.substring(0, 1)
						val otherCharacters = displayName.substring(1)

						addListItem(firstCharacter.uppercase() + otherCharacters)
					}
				}

				addOkButtonCallback("handleSelectedItem") { _: SuiEvent, parameters: Map<String, String> ->
					val selection = SuiListBox.getSelectedRow(parameters)
					val selectedDanceName = getListItem(selection).name.lowercase()
					StartDanceIntent(selectedDanceName, player, changeDance).broadcast()
				}

				display(player)
			}
		} else {
			StartDanceIntent(args, player, changeDance).broadcast()
		}
	}

	companion object {
		private const val ABILITY_NAME_PREFIX = "startDance+"
	}
}
