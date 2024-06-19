/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.network.ForceLogoutIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.player.PlayerState
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.utilities.ScheduledUtilities
import me.joshlarson.jlcommon.control.IntentChain
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.TimeUnit

class LogoutCmdCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val creature = player.creatureObject
		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) return
		creature.posture = Posture.SITTING
		Log.i("Logout command called for %s - 30s timer started", creature.objectName)
		updateLogout(player, creature, 30)
	}

	private fun updateLogout(player: Player, creature: CreatureObject, timeToLogout: Int) {
		if (!checkValidLogout(player, creature)) {
			sendSystemMessage(player, "aborted")
			return
		}

		if (timeToLogout == 0) {
			IntentChain.broadcastChain(SystemMessageIntent(player, "@logout:safe_to_log_out"), ForceLogoutIntent(player))
			return
		}
		if (isSystemMessageInterval(timeToLogout)) {
			sendSystemMessage(player, "time_left", "DI", timeToLogout)
		}
		ScheduledUtilities.run({ updateLogout(player, creature, timeToLogout - 1) }, 1, TimeUnit.SECONDS)
	}

	private fun isSystemMessageInterval(timeToLogout: Int): Boolean {
		return timeToLogout == 30 || timeToLogout == 20 || timeToLogout == 10 || timeToLogout <= 5
	}

	private fun checkValidLogout(player: Player, creature: CreatureObject): Boolean {
		if (creature.isInCombat) {
			Log.i("Logout cancelled for %s - in combat!", creature.objectName)
			return false
		}
		if (player.creatureObject !== creature) {
			Log.i("Logout cancelled for %s - Player became invalid", creature.objectName)
			return false
		}
		if (creature.posture != Posture.SITTING) {
			Log.i("Logout cancelled for %s - stood up!", creature.objectName)
			return false
		}
		if (player.playerState != PlayerState.ZONED_IN) {
			Log.i("Logout cancelled for %s - player state changed to %s", player.playerState)
			return false
		}
		return true
	}

	private fun sendSystemMessage(player: Player, str: String) {
		SystemMessageIntent(player, "@logout:$str").broadcast()
	}

	private fun sendSystemMessage(player: Player, str: String, proseKey: String, prose: Any) {
		SystemMessageIntent(player, ProsePackage(StringId("@logout:$str"), proseKey, prose)).broadcast()
	}
}
