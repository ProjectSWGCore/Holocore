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
package com.projectswg.holocore.resources.support.global.zone.sui

/**
 * Color picker with options that match the object that's being customized and the particular color of the object being
 * set (items may have multiple configurable colors).
 * @param objectId of the object that the color picker should read possible color options for.
 * These come from the associated *.pal file.
 * @param customizationVariable variable of the object to pick a color for. Objects such as armor pieces may have
 * more than one color! An example of a string could be `/private/index_color_0`
 */
class SuiColorPicker(objectId: Long, customizationVariable: String?) : SuiWindow() {

	init {
		super.suiScript = "Script.ColorPicker"
		super.title = "@base_player:swg"
		setProperty("ColorPicker", "TargetRangeMax", "500")
		setProperty("ColorPicker", "TargetNetworkId", objectId.toString())
		setProperty("ColorPicker", "TargetVariable", customizationVariable!!)
	}

	override fun onDisplayRequest() {
		// The color picker can return selection index. This index is the index in the relevant *.pal file.
		addReturnableProperty("ColorPicker", "SelectedIndex")
	}

	companion object {
		fun getSelectedIndex(parameters: Map<String, String>): Int {
			val indexRaw = parameters.getOrDefault("ColorPicker.SelectedIndex", "-1")

			return indexRaw.toInt()
		}
	}
}
