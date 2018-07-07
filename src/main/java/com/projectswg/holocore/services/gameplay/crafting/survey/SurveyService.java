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

import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.crafting.resources.ResourceListForSurveyMessage;
import com.projectswg.common.network.packets.swg.zone.crafting.resources.ResourceListForSurveyMessage.ResourceItem;
import com.projectswg.holocore.intents.gameplay.crafting.survey.SampleResourceIntent;
import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSurveyToolIntent;
import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSurveyingIntent;
import com.projectswg.holocore.resources.gameplay.crafting.survey.InProgressSampleManager;
import com.projectswg.holocore.resources.gameplay.crafting.survey.InProgressSurveyManager;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

/**
 * In charge of responding to survey requests
 */
public class SurveyService extends Service {
	
	private final InProgressSurveyManager inProgressSurveyManager;
	private final InProgressSampleManager inProgressSampleManager;
	
	public SurveyService() {
		this.inProgressSurveyManager = new InProgressSurveyManager();
		this.inProgressSampleManager = new InProgressSampleManager();
	}
	
	@IntentHandler
	private void handleStartSurveyToolIntent(StartSurveyToolIntent ssti) {
		CreatureObject creature = ssti.getCreature();
		SWGObject surveyTool = ssti.getSurveyTool();
		String resourceType = surveyTool.getTemplate().substring(47, surveyTool.getTemplate().length()-4);
		ResourceListForSurveyMessage survey = new ResourceListForSurveyMessage(creature.getObjectId(), surveyTool.getTemplate());
		RawResourceType surveyToolType = getTypeFromSurveyTool(resourceType);
		for (GalacticResource resource : GalacticResourceContainer.getContainer().getSpawnedResources(creature.getTerrain())) {
			RawResource rawResource = GalacticResourceContainer.getContainer().getRawResource(resource.getRawResourceId());
			if (!surveyToolType.isResourceType(rawResource) || rawResource.getResourceType().isResourceType(RawResourceType.CREATURE_RESOURCES))
				continue;
			survey.addResource(new ResourceItem(resource.getName(), rawResource.getName().getKey(), resource.getId()));
		}
		creature.getOwner().sendPacket(new PlayMusicMessage(0, "sound/item_surveypad_lp.snd", 1, false));
		creature.getOwner().sendPacket(survey);
	}
	
	@IntentHandler
	private void handleStartSurveyingIntent(StartSurveyingIntent ssi) {
		inProgressSurveyManager.startSession(ssi.getCreature(), ssi.getResource());
	}
	
	@IntentHandler
	private void handleStartSamplingIntent(SampleResourceIntent sri) {
		inProgressSampleManager.startSession(sri.getCreature(), sri.getResource());
	}
	
	private RawResourceType getTypeFromSurveyTool(String surveyTool) {
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
