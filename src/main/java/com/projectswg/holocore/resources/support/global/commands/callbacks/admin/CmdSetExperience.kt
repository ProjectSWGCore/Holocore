/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin

import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.log.Log

class CmdSetExperience : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val argArray = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

		if (argArray.size != 2) {
			broadcastPersonal(player, "Expected format: /setExperience <target> <xpType> <xpGained>")
			return
		}

		if (target !is CreatureObject) {
			broadcastPersonal(player, "The command must have a creature as a target")
			return
		}

		val xpType = argArray[0]
		val xpGainedRaw = argArray[1]

		try {
			val xpGained = xpGainedRaw.toInt()
			ExperienceIntent(target, xpType, xpGained).broadcast()

			Log.i("XP command: %s gave %s %d %s XP", player, target, xpGained, xpType)
		} catch (e: NumberFormatException) {
			broadcastPersonal(player, String.format("XP command: %s is not a number", xpGainedRaw))

			Log.e("XP command: %s gave a non-numerical XP gained argument of %s", player.username, xpGainedRaw)
		}
	}
}
