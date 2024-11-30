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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin.qatool

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.global.zone.DeleteCharacterIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ForceAwarenessUpdateIntent
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup
import me.joshlarson.jlcommon.log.Log

class CmdQaTool : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		if (args.indexOf(" ") == -1) {
			broadcastPersonal(player, "Invalid call to qatool: no command provided")
			return
		}
		val commandName = args.substring(0, args.indexOf(" "))

		when (commandName) {
			"help"         -> displayHelp(player)
			"force-delete" -> forceDelete(player, target)
			"recover"      -> recoverPlayer(player, args.substring(args.indexOf(' ') + 1))
			"setinstance"  -> setInstance(player, args.substring(args.indexOf(' ') + 1))
			"details"      -> QaToolDetails.sendDetails(player, target, args)
			else           -> createPrimaryWindow(player)
		}
		Log.i("%s has accessed the QA Tool", player.username)
	}

	private fun createPrimaryWindow(player: Player) {
		SuiListBox().run {
			title = "QA Tool"
			prompt = "Select an action"

			addListItem(1, 0, player, "Access Level 0")
			addListItem(2, 5, player, "Access Level 5")
			addListItem(3, 10, player, "Access Level 10")
			addListItem(4, 15, player, "Access Level 15")
			addListItem(5, 20, player, "Access Level 20")
			addListItem(6, 25, player, "Access Level 25")
			addListItem(7, 30, player, "Access Level 30")
			addCallback(SuiEvent.OK_PRESSED, "") { _: SuiEvent, parameters: Map<String, String> ->
				val index = SuiListBox.getSelectedRow(parameters)
				if (index < 0 || index >= list.size) return@addCallback
				handlePrimarySelection(player, getListItem(index).id.toInt())
			}
			display(player)
		}
	}

	private fun handlePrimarySelection(player: Player, id: Int) {
		Log.d("Player %s selected %d", player, id)
		when (id) {
			1, 2, 3, 4, 5, 6, 7 -> {}
			else                -> {}
		}
	}

	/* Handlers */
	private fun forceDelete(player: Player, target: SWGObject?) {
		if (target == null)
			return
		SuiMessageBox().run {
			title = "Force Delete?"
			prompt = "Are you sure you want to delete this object?"
			addOkButtonCallback("handleDeleteObject") { _: SuiEvent, _: Map<String, String> ->
				if (target is CreatureObject && target.isPlayer) {
					Log.i("[%s] Requested deletion of character: %s", player.username, target.getObjectName())
					DeleteCharacterIntent((target as CreatureObject?)!!).broadcast()
				} else {
					Log.i("[%s] Requested deletion of object: %s", player.username, target)
					DestroyObjectIntent(target).broadcast()
				}
			}
			display(player)
		}
	}

	private fun recoverPlayer(player: Player, args: String) {
		var args = args
		args = args.trim { it <= ' ' }
		val recoveree = PlayerLookup.getCharacterByFirstName(args)
		if (recoveree == null) {
			broadcastPersonal(player, "Could not find player by first name: '$args'")
			return
		}

		val loc = Location(3525.0, 4.0, -4807.0, Terrain.TATOOINE)
		recoveree.posture = Posture.UPRIGHT
		recoveree.moveToContainer(null, loc)
		broadcastPersonal(player, "Sucessfully teleported " + recoveree.objectName + " to " + loc.position)
	}

	private fun setInstance(player: Player, args: String) {
		val target: CreatureObject
		val instanceString: String
		if (args.contains(' ', ignoreCase = false)) {
			val commandArguments = args.trim { it <= ' ' }.split(' ')
			if (commandArguments.size != 2) {
				broadcastPersonal(player, "Invalid setinstance command. Expected two arguments: <playername> <instance>")
				return
			}
			val playerSearch = PlayerLookup.getCharacterByFirstName(commandArguments[0])
			if (playerSearch == null) {
				broadcastPersonal(player, "Could not find player by first name: '${commandArguments[0]}'")
				return
			}
			target = playerSearch
			instanceString = commandArguments[1]
		} else {
			target = player.creatureObject
			instanceString = args
		}
		
		try {
			if (target.parent != null) {
				MoveObjectIntent(target, target.worldLocation, 0.0).broadcast()
			}
			target.setInstance(target.instanceLocation.instanceType, instanceString.toInt())
			ForceAwarenessUpdateIntent(target).broadcast()
		} catch (e: NumberFormatException) {
			Log.e("Invalid instance number with qatool: %s", instanceString)
			broadcastPersonal(player, "Invalid call to qatool: '$instanceString' - invalid instance number")
		}
	}

	private fun displayHelp(player: Player) {
		val prompt = """
			The following are acceptable arguments that can be used as shortcuts to the various QA tools:
			help -- Displays this window
			
			""".trimIndent()
		SuiMessageBox().run {
			this.title = "QA Tool - Help"
			this.prompt = prompt
			this.buttons = SuiButtons.OK
			display(player)
		}
	}

	companion object {
		/* Utility Methods */
		private fun SuiListBox.addListItem(id: Int, accessLevel: Int, player: Player, name: String) {
			if (player.accessLevel.value >= accessLevel)
				addListItem(name, id.toLong(), null)
		}
	}
}
