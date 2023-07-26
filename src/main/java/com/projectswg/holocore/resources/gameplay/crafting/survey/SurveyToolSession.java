/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.crafting.survey;

import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.common.network.packets.swg.zone.crafting.resources.ResourceListForSurveyMessage;
import com.projectswg.common.network.packets.swg.zone.crafting.resources.ResourceListForSurveyMessage.ResourceItem;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.log.Log;

public class SurveyToolSession {
	
	private final CreatureObject creature;
	private final TangibleObject surveyTool;
	private final SurveyHandler surveyHandler;
	private final SampleHandler sampleHandler;
	
	public SurveyToolSession(CreatureObject creature, TangibleObject surveyTool, ScheduledThreadPool executor) {
		this.creature = creature;
		this.surveyTool = surveyTool;
		this.surveyHandler = new SurveyHandler(creature, surveyTool, executor);
		this.sampleHandler = new SampleHandler(creature, surveyTool, executor);
	}
	
	public synchronized void startSession() {
		surveyHandler.startSession();
		sampleHandler.startSession();
		
		creature.sendSelf(new PlayMusicMessage(0, "sound/item_surveypad_lp.snd", 1, false));
		
		String resourceType = surveyTool.getTemplate().substring(47, surveyTool.getTemplate().length()-4);
		ResourceListForSurveyMessage survey = new ResourceListForSurveyMessage(creature.getObjectId(), surveyTool.getTemplate());
		RawResourceType surveyToolType = getTypeFromSurveyTool(resourceType);
		for (GalacticResource resource : GalacticResourceContainer.INSTANCE.getSpawnedResources(creature.getTerrain())) {
			RawResource rawResource = GalacticResourceContainer.INSTANCE.getRawResource(resource.getRawResourceId());
			if (!surveyToolType.isResourceType(rawResource) || rawResource.getResourceType().isResourceType(RawResourceType.CREATURE_RESOURCES))
				continue;
			survey.addResource(new ResourceItem(resource.getName(), rawResource.getName(), resource.getId()));
		}
		creature.sendSelf(survey);
	}
	
	public synchronized void stopSession() {
		surveyHandler.stopSession();
		sampleHandler.stopSession();
	}
	
	public synchronized void onPlayerMoved() {
		sampleHandler.onPlayerMoved();
	}
	
	public synchronized void startSurvey(GalacticResource resource) {
		if (sampleHandler.isSampling()) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:survey_sample")); // "You can't take a survey while you are collecting samples."
			return;
		}
		surveyHandler.startSurvey(resource);
	}
	
	public synchronized void startSample(GalacticResource resource) {
		if (surveyHandler.isSurveying()) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_survey")); // "You can't take resource samples while you are surveying"
			return;
		}
		if (surveyHandler.getLastResourceSurveyed() != resource) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "You can't get a sample without having scanned the area first"));
			return;
		}
		sampleHandler.startSampleLoop(resource);
	}
	
	public synchronized void stopSample() {
		sampleHandler.stopSampleLoop();
	}
	
	private static RawResourceType getTypeFromSurveyTool(String surveyTool) {
		switch (surveyTool) {
			case "mineral":
				return RawResourceType.MINERAL;
			case "lumber":
				return RawResourceType.FLORA_STRUCTURAL;
			case "liquid":
				return RawResourceType.CHEMICAL;
			case "moisture":
				return RawResourceType.WATER;
			case "gas":
			case "gas_thermal":
				return RawResourceType.GAS;
			case "wind":
				return RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND;
			case "solar":
				return RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR;
			case "inorganic":
				return RawResourceType.INORGANIC;
			case "organic":
				return RawResourceType.ORGANIC;
			default:
				Log.w("Unknokwn survey tool type: %s", surveyTool);
			case "all":
				return RawResourceType.RESOURCE;
		}
	}
	
}
