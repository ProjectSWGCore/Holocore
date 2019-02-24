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
package com.projectswg.holocore.services.gameplay.crafting.survey;

import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSamplingIntent;
import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSurveyToolIntent;
import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSurveyingIntent;
import com.projectswg.holocore.intents.gameplay.crafting.survey.StopSamplingIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.resources.gameplay.crafting.survey.SurveyToolSession;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In charge of responding to survey requests
 */
public class SurveyToolService extends Service {
	
	private final Map<CreatureObject, SurveyToolSession> surveySessions;
	private final ScheduledThreadPool executor;
	
	public SurveyToolService() {
		this.surveySessions = new ConcurrentHashMap<>();
		this.executor = new ScheduledThreadPool(1, "survey-tool-service");
	}
	
	@Override
	public boolean start() {
		executor.start();
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handleStartSurveyToolIntent(StartSurveyToolIntent ssti) {
		CreatureObject creature = ssti.getCreature();
		
		SurveyToolSession session = new SurveyToolSession(creature, ssti.getSurveyTool(), executor);
		SurveyToolSession prevSession = surveySessions.put(creature, session);
		if (prevSession != null)
			prevSession.stopSession();
		session.startSession();
	}
	
	@IntentHandler
	private void handlePlayerTransformedIntent(PlayerTransformedIntent pti) {
		SurveyToolSession session = surveySessions.get(pti.getPlayer());
		if (session != null)
			session.onPlayerMoved();
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		if (creature != null) {
			SurveyToolSession session = surveySessions.get(creature);
			if (session != null) {
				session.stopSession();
			}
		}
	}
	
	@IntentHandler
	private void handleStartSurveyingIntent(StartSurveyingIntent ssi) {
		CreatureObject creature = ssi.getCreature();
		SurveyToolSession session = surveySessions.get(creature);
		if (session == null) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "Unable to survey, no survey tool has been opened yet"));
			return;
		}
		
		session.startSurvey(ssi.getResource());
	}
	
	@IntentHandler
	private void handleStartSamplingIntent(StartSamplingIntent sri) {
		CreatureObject creature = sri.getCreature();
		SurveyToolSession session = surveySessions.get(creature);
		if (session == null) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "Unable to sample, no survey tool has been opened yet"));
			return;
		}
		
		session.startSample(sri.getResource());
	}
	
	@IntentHandler
	private void handleStopSamplingIntent(StopSamplingIntent sri) {
		CreatureObject creature = sri.getCreature();
		SurveyToolSession session = surveySessions.get(creature);
		if (session != null)
			session.stopSession();
	}
	
}
