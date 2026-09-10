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
package com.projectswg.holocore.services.gameplay.player.experience

import com.projectswg.common.data.encodables.oob.OutOfBandPackage
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class ExperiencePointService : Service() {

	private val xpMultiplier = PswgDatabase.config.getDouble(this, "xpMultiplier", 1.0)

	@IntentHandler
	private fun handleExperienceIntent(ei: ExperienceIntent) {
		val creatureObject = ei.creatureObject ?: return
		val playerObject = creatureObject.playerObject ?: return
		val xpType = ei.xpType ?: return
		var experienceGained = ei.experienceGained

		if (ei.isXpMultiplied) {
			experienceGained = (experienceGained * xpMultiplier).toInt()
		}

		awardExperience(creatureObject, ei.flytextTarget, playerObject, xpType, experienceGained)
	}

	private fun awardExperience(creatureObject: CreatureObject, flytextTarget: SWGObject?, playerObject: PlayerObject, xpType: String, xpGained: Int) {
		val currentXp = playerObject.getExperiencePoints(xpType)
		val xpLimit = getXpLimit(creatureObject, xpType)

		if (xpGained > 0 && currentXp >= xpLimit) {
			StandardLog.onPlayerTrace(this, creatureObject, "gained no %s XP, already at the limit of %d", xpType, xpLimit)
			return
		}

		val newXpTotal = minOf(currentXp + xpGained, xpLimit)
		val actualXpGained = newXpTotal - currentXp

		playerObject.setExperiencePoints(xpType, newXpTotal)
		StandardLog.onPlayerTrace(this, creatureObject, "gained %d %s XP", actualXpGained, xpType)

		if (newXpTotal == xpLimit && xpGained > 0) {
			StandardLog.onPlayerTrace(this, creatureObject, "reached the %s XP limit of %d", xpType, xpLimit)
		}

		if (xpType != "combat_general") {
			showFlytext(creatureObject, flytextTarget, actualXpGained)
			showSystemMessage(creatureObject, xpType)
		}
	}

	/**
	 * The limit is the largest cap among the skills the player has for this XP type. Players without any such skill are
	 * held to the default limit from the client datatable, or [UNKNOWN_XP_TYPE_LIMIT] if the XP type has no default.
	 */
	private fun getXpLimit(creatureObject: CreatureObject, xpType: String): Int {
		val skillLimit = creatureObject.skills
			.mapNotNull { ServerData.skills.getSkillByName(it) }
			.filter { it.xpType == xpType && it.xpCap > 0 }
			.maxOfOrNull { it.xpCap }

		return skillLimit ?: ServerData.xpLimits.getLimit(xpType) ?: UNKNOWN_XP_TYPE_LIMIT
	}

	private fun showSystemMessage(creatureObject: CreatureObject, xpType: String) {
		// TODO display different messages with inspiration bonus and/or group bonus
		val owner = creatureObject.owner ?: return
		val xpTypeDisplayName = StringId("exp_n", xpType)
		val message = ProsePackage(StringId("base_player", "prose_grant_xp"), "TO", xpTypeDisplayName)
		SystemMessageIntent.broadcastPersonal(owner, message, ChatSystemMessage.SystemChatType.CHAT_BOX)
	}

	private fun showFlytext(creatureObject: CreatureObject, flytextTarget: SWGObject?, xpGained: Int) {
		if (flytextTarget == null) {
			return
		}

		val message = OutOfBandPackage(ProsePackage(StringId("base_player", "prose_flytext_xp"), "DI", xpGained))
		val packet = ShowFlyText(flytextTarget.objectId, message, ShowFlyText.Scale.MEDIUM, SWGColor.Violets.magenta)
		creatureObject.sendSelf(packet)
	}

	private companion object {
		private const val UNKNOWN_XP_TYPE_LIMIT = 2000
	}
}
