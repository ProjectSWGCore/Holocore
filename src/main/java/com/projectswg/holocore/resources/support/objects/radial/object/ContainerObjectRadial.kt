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
package com.projectswg.holocore.resources.support.objects.radial.`object`

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiInputBox
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

class ContainerObjectRadial : SWGObjectRadial() {
	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		super.getOptions(options, player, target)

		// Only show the rename option if the requester has admin access to the targeted container
		val containerPermissions = target.containerPermissions
		val creatureObject = player.creatureObject

		if (containerPermissions.canMove(creatureObject, target)) {
			options.add(RadialOption.create(RadialItem.SET_NAME))
		}
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		val containerPermissions = target.containerPermissions
		val creatureObject = player.creatureObject

		if (!containerPermissions.canMove(creatureObject, target)) {
			return
		}

		if (selection == RadialItem.SET_NAME) {
			SuiInputBox().run {
				title = "@sui:set_name_title"
				prompt = "@sui:set_name_prompt"
				addOkButtonCallback("rename_ok") { _: SuiEvent, parameters: Map<String, String> -> handleRename(parameters, target) }
				setPropertyText("txtInput", target.objectName) // Insert the current name into the text fieldsui.display(player);
				display(player)
			}
		}
	}

	private fun handleRename(parameters: Map<String, String>, target: SWGObject) {
		val enteredText: String = SuiInputBox.getEnteredText(parameters)

		target.setObjectName(enteredText)
	}
}
