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
package com.projectswg.holocore.services.gameplay.crafting.survey

import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType
import com.projectswg.holocore.intents.gameplay.crafting.StartSamplingIntent
import com.projectswg.holocore.intents.gameplay.crafting.StartSurveyToolIntent
import com.projectswg.holocore.intents.gameplay.crafting.StartSurveyingIntent
import com.projectswg.holocore.intents.gameplay.crafting.StopSamplingIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent
import com.projectswg.holocore.resources.gameplay.crafting.survey.SurveyToolSession
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * In charge of responding to survey requests
 */
class SurveyToolService : Service() {
	private val surveySessions: MutableMap<CreatureObject, SurveyToolSession> = ConcurrentHashMap()
	private val surveySessionScope = CoroutineScope(context = Dispatchers.Default)

	override fun start(): Boolean {
		return true
	}

	override fun stop(): Boolean {
		surveySessionScope.cancel()
		return super.stop()
	}

	@IntentHandler
	private fun handleStartSurveyToolIntent(ssti: StartSurveyToolIntent) {
		val creature = ssti.creature

		val session = SurveyToolSession(creature, ssti.surveyTool, surveySessionScope)
		val prevSession = surveySessions.put(creature, session)
		prevSession?.stopSession()
		session.startSession()
	}

	@IntentHandler
	private fun handlePlayerTransformedIntent(pti: PlayerTransformedIntent) {
		val session = surveySessions[pti.player]
		session?.onPlayerMoved()
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		val creature = pei.player.creatureObject
		if (creature != null) {
			val session = surveySessions[creature]
			session?.stopSession()
		}
	}

	@IntentHandler
	private fun handleStartSurveyingIntent(ssi: StartSurveyingIntent) {
		val creature = ssi.creature
		val session = surveySessions[creature]
		if (session == null) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "Unable to survey, no survey tool has been opened yet"))
			return
		}

		session.startSurvey(ssi.resource)
	}

	@IntentHandler
	private fun handleStartSamplingIntent(sri: StartSamplingIntent) {
		val creature = sri.creature
		val session = surveySessions[creature]
		if (session == null) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "Unable to sample, no survey tool has been opened yet"))
			return
		}

		session.startSample(sri.resource)
	}

	@IntentHandler
	private fun handleStopSamplingIntent(sri: StopSamplingIntent) {
		val creature = sri.creature
		val session = surveySessions[creature]
		session?.stopSession()
	}
}
