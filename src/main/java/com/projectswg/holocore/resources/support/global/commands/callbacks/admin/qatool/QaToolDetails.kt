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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin.qatool

import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.holocore.intents.gameplay.combat.IncapacitateCreatureIntent
import com.projectswg.holocore.intents.gameplay.combat.KillCreatureIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.IntentChain

object QaToolDetails {
	fun sendDetails(player: Player, target: SWGObject?, args: String) {
		var target = target
		val split = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		if (target == null) {
			target = player.creatureObject
		}
		if (matchesCommand(split, 2, "observers")) {
			broadcastPersonal(player, "Observers: " + target!!.observers)
			return
		}
		if (matchesCommand(split, 3, "aware-of")) {
			val aware: Collection<SWGObject> = target!!.objectsAware
			var count = 0
			for (obj in aware) {
				if (obj.objectId == split[2].toLong() || obj.template.contains(split[2])) {
					broadcastPersonal(player, "True: $obj")
					return
				}
				count++
			}
			broadcastPersonal(player, "False. Checked $count in aware")
			return
		}
		if (matchesCommand(split, 2, "deathblow")) {
			broadcastPersonal(player, "Dealing deathblow")
			val creo = target as CreatureObject?
			IntentChain.broadcastChain(
				IncapacitateCreatureIntent(creo!!, creo), KillCreatureIntent(creo, creo)
			)
			return
		}

		sendPersonal(player, "%s - %s [%d]", target!!.objectName, target.javaClass.simpleName, target.objectId)
		sendPersonal(player, "    STR:            %s / %s", target.stringId, target.detailStringId)
		sendPersonal(player, "    Template:       %s", target.template)
		sendPersonal(player, "    GOT:            %s", target.gameObjectType)
		if (target is CreatureObject) {
			val creo = target
			sendPersonal(player, "    Health:         %d / %d", creo.health, creo.maxHealth)
			sendPersonal(player, "    Action:         %d / %d", creo.action, creo.maxAction)
		}
		if (target is TangibleObject) {
			sendPersonal(player, "    PVP Flags:      %d", target.pvpFlags)
		}
	}

	private fun matchesCommand(args: Array<String>, argLength: Int, command: String): Boolean {
		return args.size >= argLength && args[1].equals(command, ignoreCase = true)
	}

	private fun sendPersonal(player: Player, format: String, vararg args: Any) {
		if (args.isEmpty()) player.sendPacket(ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT_BOX, format))
		else player.sendPacket(ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT_BOX, String.format(format, *args)))
	}
}
