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
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiColorPicker
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject

class SpecialEditionGogglesRadial(private val colorableFrame: Boolean) : SWGObjectRadial() {

	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		options.add(RadialOption.createSilent(RadialItem.EXAMINE))

		if (colorableFrame) {
			options.add(RadialOption.create(RadialItem.SERVER_MENU1, "@sui:set_color", RadialOption.create(RadialItem.SERVER_MENU2, "@sui:color_frame"), RadialOption.create(RadialItem.SERVER_MENU3, "@sui:color_lens")))
		} else {
			options.add(RadialOption.create(RadialItem.SERVER_MENU1, "@sui:set_color", RadialOption.create(RadialItem.SERVER_MENU2, "@sui:color_lens")))
		}
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		when (selection) {
			RadialItem.SERVER_MENU1 -> SystemMessageIntent.broadcastPersonal(player, "@error_message:goggle_submenu")
			RadialItem.SERVER_MENU2 -> showColorPicker(player, target, "/private/index_color_1")
			RadialItem.SERVER_MENU3 -> showColorPicker(player, target, "/private/index_color_2")
			else                    -> {}
		}
	}

	private fun showColorPicker(player: Player, target: SWGObject, customizationVariable: String) {
		SuiColorPicker(target.objectId, customizationVariable).run {
			addOkButtonCallback("ok") { _: SuiEvent, parameters: Map<String, String> ->
				if (target is TangibleObject) {
					val selectedIndex: Int = SuiColorPicker.getSelectedIndex(parameters)
					target.putCustomization(customizationVariable, selectedIndex)
				}
			}

			display(player)
		}
	}
}
