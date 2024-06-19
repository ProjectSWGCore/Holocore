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
package com.projectswg.holocore.resources.support.objects.radial.`object`.survey

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.holocore.intents.gameplay.crafting.StartSurveyToolIntent
import com.projectswg.holocore.resources.gameplay.crafting.survey.SurveyToolResolution
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.objects.radial.`object`.UsableObjectRadial
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.log.Log

class ObjectSurveyToolRadial : UsableObjectRadial() {
	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		val creature = player.creatureObject ?: return

		val resolutions: List<SurveyToolResolution> = SurveyToolResolution.getOptions(creature)
		val toolOptions: Array<RadialOption> = getResolutions(resolutions)

		options.add(RadialOption.create(RadialItem.ITEM_USE))
		if (toolOptions.isEmpty()) options.add(RadialOption.create(RadialItem.SERVER_ITEM_OPTIONS, "Tool Options"))
		else options.add(RadialOption.create(RadialItem.SERVER_SURVEY_TOOL_RANGE, "Tool Options", *toolOptions))
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		if (target !is TangibleObject) return
		val creature = player.creatureObject ?: return
		val resolutions: List<SurveyToolResolution> = SurveyToolResolution.getOptions(creature)
		if (resolutions.isEmpty()) {
			creature.sendSelf(ChatSystemMessage(ChatSystemMessage.SystemChatType.PERSONAL, "@error_message:survey_cant"))
			return
		}

		when (selection) {
			RadialItem.ITEM_USE                                                 -> if (target.getServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE) == null) startToolOptionMenu(player, target, true)
			else StartSurveyToolIntent(creature, target).broadcast()

			RadialItem.SERVER_SURVEY_TOOL_RANGE, RadialItem.SERVER_ITEM_OPTIONS -> startToolOptionMenu(player, target, false)
			RadialItem.SERVER_MENU1                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 1)
			RadialItem.SERVER_MENU2                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 2)
			RadialItem.SERVER_MENU3                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 3)
			RadialItem.SERVER_MENU4                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 4)
			RadialItem.SERVER_MENU5                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 5)
			RadialItem.SERVER_MENU6                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 6)
			RadialItem.SERVER_MENU7                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 7)
			RadialItem.SERVER_MENU8                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 8)
			RadialItem.SERVER_MENU9                                             -> target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 9)
			else                                                                -> Log.t("Used unknown selection: %s", selection)
		}
	}

	private fun getResolutions(resolutions: List<SurveyToolResolution>): Array<RadialOption> {
		val options: MutableList<RadialOption> = ArrayList()
		val menuItems: Array<RadialItem> = arrayOf(
			RadialItem.SERVER_MENU1,
			RadialItem.SERVER_MENU2,
			RadialItem.SERVER_MENU3,
			RadialItem.SERVER_MENU4,
			RadialItem.SERVER_MENU5,
			RadialItem.SERVER_MENU6,
			RadialItem.SERVER_MENU7,
			RadialItem.SERVER_MENU8,
			RadialItem.SERVER_MENU9,
		)

		for (resolution in resolutions) {
			val menuItemIndex: Int = resolution.counter - 1
			if (menuItemIndex < 0 || menuItemIndex >= menuItems.size) continue
			options.add(RadialOption.create(menuItems[menuItemIndex], String.format("%dm x %dpts", resolution.range, resolution.resolution)))
		}

		return options.toTypedArray()
	}

	private fun startToolOptionMenu(player: Player, surveyTool: TangibleObject, startToolWhenClosed: Boolean) {
		val creature: CreatureObject = player.creatureObject ?: return

		val resolutions: List<SurveyToolResolution> = SurveyToolResolution.getOptions(creature)
		if (resolutions.isEmpty()) return

		SuiListBox().run {
			title = "STAR WARS GALAXIES"
			prompt = "@survey:select_range"
			for (resolution in resolutions) {
				addListItem(String.format("%dm x %dpts", resolution.range, resolution.resolution), resolution)
			}
			addOkButtonCallback("rangeSelected") { _: SuiEvent, params: Map<String, String> ->
				val selectedRow: Int = SuiListBox.getSelectedRow(params)
				if (selectedRow < 0 || selectedRow >= list.size)
					return@addOkButtonCallback
				val item = getListItem(selectedRow)
				val resolution: SurveyToolResolution = item.obj as SurveyToolResolution
				surveyTool.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, resolution.counter)
				if (startToolWhenClosed) StartSurveyToolIntent(creature, surveyTool).broadcast()
			}
			display(player)
		}
	}
}
