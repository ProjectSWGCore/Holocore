/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.network.packets.swg.zone.object_controller.PlayerEmote
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup

class SocialInternalCmdCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		// Args: targetId (0), emoteId (1), animation (2), text (3)
		val cmd = args.split(" ".toRegex(), limit = 4).toTypedArray()
		if (cmd.size != 4)
			return
		val chatTarget = if (cmd[0] == "0") target else ObjectLookup.getObjectById(cmd[0].toLong())

		val emoteId = cmd[1].toInt()
		val sourceId = player.creatureObject.objectId
		val targetId = (if (chatTarget == null || emoteId == 178) 0 else chatTarget.objectId)
		val animation = cmd[2] == "1"
		val text = cmd[3] == "1" && emoteId != 178 // don't show the text for jump
		val emoteFlags = ((if (text) 2 else 0) or (if (animation) 1 else 0)).toByte()

		for (aware in player.creatureObject.observers) {
			aware.sendPacket(PlayerEmote(aware.creatureObject.objectId, sourceId, targetId, emoteId, emoteFlags))
		}
	}
}
