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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.intents.gameplay.crafting.StopSamplingIntent
import com.projectswg.holocore.intents.gameplay.entertainment.StopDanceIntent
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.commands.Locomotion
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState

class StandCmdCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val creature = player.creatureObject

		if (Locomotion.KNOCKED_DOWN.isActive(creature)) {
			return
		}

		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) {
			return
		}

		if (creature.isPerforming) {
			// Ziggy: When you move while dancing, the client wants to execute /stand instead of /stopDance. Blame SOE.
			StopDanceIntent(player).broadcast()
		} else {
			creature.clearStatesBitmask(CreatureState.SITTING_ON_CHAIR)
			creature.posture = Posture.UPRIGHT
			creature.setMovementPercent(1.0)
			creature.setTurnScale(1.0)
			StopSamplingIntent(creature).broadcast()
		}

		if ("meditating" == creature.moodAnimation) {
			creature.moodAnimation = "neutral"
		}
	}
}
