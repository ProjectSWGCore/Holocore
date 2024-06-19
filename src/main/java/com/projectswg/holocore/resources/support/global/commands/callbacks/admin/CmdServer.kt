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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin

import com.projectswg.common.data.encodables.tangible.PvpFlag
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.intents.support.data.control.BanPlayerIntent
import com.projectswg.holocore.intents.support.data.control.KickPlayerIntent
import com.projectswg.holocore.intents.support.data.control.ShutdownServerIntent
import com.projectswg.holocore.intents.support.data.control.UnbanPlayerIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiInputBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.TimeUnit

class CmdServer : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		SuiListBox().run {
			title = "Server Management"
			prompt = "Select the management function you wish to perform from the list."

			addListItem("Kick Player")
			addListItem("Ban Player")
			addListItem("Unban Player")
			addListItem("Shutdown Server - 15 Minutes")
			addListItem("Shutdown Server - Custom Time")
			addListItem("Spawn lair")

			addOkButtonCallback("handleSelectedItem") { _: SuiEvent, parameters: Map<String, String> ->
				when (val selection = SuiListBox.getSelectedRow(parameters)) {
					0    -> handleKickPlayer(player)
					1    -> handleBanPlayer(player)
					2    -> handleUnbanPlayer(player)
					3    -> handleShutdownServer(player)
					4    -> handleCustomShutdownServer(player)
					5    -> handleSpawnLair(player)
					else -> Log.i("There is no handle function for selected list item %d", selection)
				}
			}
			display(player)
		}
	}

	private fun handleSpawnLair(player: Player) {
		val lair = ObjectCreator.createObjectFromTemplate(ClientFactory.formatToSharedFile("object/tangible/lair/bark_mite/lair_bark_mite.iff")) as TangibleObject
		lair.objectName = "a bark mite lair"
		lair.removeOptionFlags(OptionFlag.INVULNERABLE)
		lair.setPvpFlags(PvpFlag.YOU_CAN_ATTACK)
		lair.addOptionFlags(OptionFlag.HAM_BAR)
		lair.location = player.creatureObject.location
		ObjectCreatedIntent(lair).broadcast()
	}

	companion object {
		private fun handleKickPlayer(player: Player) {
			SuiInputBox().run {
				title = "Kick Player"
				prompt = "Enter the name of the player that you wish to KICK from the server."
				addOkButtonCallback("handleKickPlayer") { _: SuiEvent, parameters: Map<String, String> ->
					val name = SuiInputBox.getEnteredText(parameters)
					KickPlayerIntent(player, name).broadcast()
				}
				display(player)
			}
		}

		private fun handleBanPlayer(player: Player) {
			SuiInputBox().run {
				title = "Ban Player"
				prompt = "Enter the name of the player that you wish to BAN from the server."
				addOkButtonCallback("handleBanPlayer") { _: SuiEvent, parameters: Map<String, String> -> BanPlayerIntent(player, SuiInputBox.getEnteredText(parameters)).broadcast() }
				display(player)
			}
		}

		private fun handleUnbanPlayer(player: Player) {
			SuiInputBox().run {
				title = "Unban Player"
				prompt = "Enter the name of the player that you wish to UNBAN from the server."
				addOkButtonCallback("handleUnbanPlayer") { _: SuiEvent, parameters: Map<String, String> -> UnbanPlayerIntent(player, SuiInputBox.getEnteredText(parameters)).broadcast() }
				display(player)
			}
		}

		private fun handleShutdownServer(player: Player) {
			SuiMessageBox().run {
				title = "Shutdown Server"
				prompt = "Are you sure you wish to begin the shutdown sequence?"
				buttons = SuiButtons.YES_NO
				addOkButtonCallback("handleShutdownServer") { _, _ -> ShutdownServerIntent(15, TimeUnit.MINUTES).broadcast() }
				display(player)
			}
		}

		private fun handleCustomShutdownServer(player: Player) {
			SuiListBox().run {
				title = "Shutdown Server"
				prompt = "Select the time unit that the time will be specified in."
				// Ziggy: Add all the TimeUnit values as options
				val unitValues = TimeUnit.entries
				for (i in unitValues.indices)
					addListItem(unitValues[i].toString(), i)

				addOkButtonCallback("handleCustomShutdownTime") { _: SuiEvent, parameters: Map<String, String> ->
					val index = SuiListBox.getSelectedRow(parameters)
					if (index < 0 || index >= unitValues.size)
						return@addOkButtonCallback
					val timeUnit = unitValues[index]

					// Let the admin enter the amount of time until shutdown, in timeUnit's
					SuiInputBox().run {
						title = "Shutdown Server"
						prompt = "Enter the time until the server shuts down. The shutdown sequence will begin upon hitting OK."

						allowStringCharacters(false)
						addOkButtonCallback("handleCustomShutdownCountdown") { _: SuiEvent, parameters: Map<String, String> ->
							val countdown = SuiInputBox.getEnteredText(parameters).toLong()
							ShutdownServerIntent(countdown, timeUnit).broadcast()
						}
						display(player)
					}
				}

				display(player)
			}
		}
	}
}
