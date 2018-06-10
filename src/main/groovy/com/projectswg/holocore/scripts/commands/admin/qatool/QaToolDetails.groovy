/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.scripts.commands.admin.qatool

import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.holocore.intents.gameplay.combat.IncapacitateCreatureIntent
import com.projectswg.holocore.intents.gameplay.combat.KillCreatureIntent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.utilities.IntentFactory

static def sendDetails(Player player, SWGObject object, String [] args) {
	if (object == null) {
		object = player.getCreatureObject()
	}
	if (matchesCommand(args, 2, "observers")) {
		IntentFactory.sendSystemMessage(player, "Observers: " + object.getObservers())
		return
	}
	if (matchesCommand(args, 3, "aware-of")) {
		def aware = object.getObjectsAware()
		def count = 0
		for (def iterator = aware.iterator(); iterator.hasNext();) {
			def obj = ++iterator
			if (obj.getObjectId() == parseInt(args[2]) || obj.getTemplate().contains(args[2])) {
				IntentFactory.sendSystemMessage(player, "True: " + obj)
				return
			}
			count++
		}
		IntentFactory.sendSystemMessage(player, "False. Checked " + count + " in aware")
		return
	}
	if (matchesCommand(args, 2, "deathblow")) {
		IntentFactory.sendSystemMessage(player, "Dealing deathblow")
		def creo = object as CreatureObject
		def incap = new IncapacitateCreatureIntent(creo, creo)
		new KillCreatureIntent(creo, creo).broadcastAfterIntent(new IncapacitateCreatureIntent(creo, creo))
		incap.broadcast()
		return
	}
	
	sendPersonal(player, "%s - %s [%d]", object.getObjectName(), object.getClass().getSimpleName(), object.getObjectId())
	sendPersonal(player, "    STR:            %s / %s", object.getStringId(), object.getDetailStringId())
	sendPersonal(player, "    Template:       %s", object.getTemplate())
	sendPersonal(player, "    GOT:            %s", object.getGameObjectType())
	sendPersonal(player, "    Classification: %s", object.getGenerated())
	sendPersonal(player, "    Load Range:     %.0f", object.getLoadRange())
	if (object instanceof CreatureObject) {
		sendPersonal(player, "    Health:         %d / %d", object.getHealth(), object.getMaxHealth())
		sendPersonal(player, "    Action:         %d / %d", object.getAction(), object.getMaxAction())
	}
	if (object instanceof TangibleObject) {
		sendPersonal(player, "    PVP Flags:      %d", object.getPvpFlags())
	}
}

static def matchesCommand(String [] args, int argLength, String command) {
	return args.length >= argLength && args[1].equalsIgnoreCase(command)
}

static def sendPersonal(Player player, String format, Object ... args) {
	if (args.length == 0)
		player.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT_BOX, format))
	else
		player.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT_BOX, String.format(format, args)))
}
