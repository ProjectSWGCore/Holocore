/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.crafting.survey;

import com.projectswg.common.debug.Log;

import network.packets.swg.zone.crafting.resources.ResourceListForSurveyMessage;
import network.packets.swg.zone.crafting.resources.ResourceListForSurveyMessage.ResourceItem;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import services.crafting.resource.galactic.GalacticResource;
import services.crafting.resource.galactic.GalacticResourceType;
import services.crafting.resource.galactic.storage.GalacticResourceContainer;
import services.crafting.resource.raw.RawResource;

public class SurveySession {
	
	private final CreatureObject creature;
	private final SWGObject surveyTool;
	
	public SurveySession(CreatureObject creature, SWGObject surveyTool) {
		this.creature = creature;
		this.surveyTool = surveyTool;
	}
	
	public SWGObject getSurveyTool() {
		return surveyTool;
	}
	
	public void startSession() {
		String resourceType = surveyTool.getTemplate().substring(47, surveyTool.getTemplate().length()-4);
		Log.d("%s starting survey session for %s", creature.getObjectName(), resourceType);
		ResourceListForSurveyMessage survey = new ResourceListForSurveyMessage(creature.getObjectId(), surveyTool.getTemplate());
		GalacticResourceType surveyToolType = getTypeFromSurveyTool(resourceType);
		for (GalacticResource resource : GalacticResourceContainer.getContainer().getSpawnedResources(creature.getTerrain())) {
			RawResource rawResource = GalacticResourceContainer.getContainer().getRawResource(resource.getRawResourceId());
			if (!surveyToolType.isResourceType(rawResource))
				continue;
			survey.addResource(new ResourceItem(resource.getName(), rawResource.getName().getKey(), resource.getId()));
		}
		creature.getOwner().sendPacket(survey);
	}
	
	public void stopSession() {
		Log.d("%s ending survey session with %s", creature.getObjectName(), surveyTool);
	}
	
	private GalacticResourceType getTypeFromSurveyTool(String surveyTool) {
		switch (surveyTool) {
			case "mineral":
				return GalacticResourceType.MINERAL;
			case "lumber":
				return GalacticResourceType.FLORA_STRUCTURAL;
			case "liquid":
			case "moisture":
				return GalacticResourceType.WATER;
			case "gas":
			case "gas_thermal":
				return GalacticResourceType.GAS;
			case "wind":
				return GalacticResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND;
			case "solar":
				return GalacticResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR;
			case "inorganic":
				return GalacticResourceType.INORGANIC;
			case "organic":
				return GalacticResourceType.ORGANIC;
			default:
				Log.w("Unknokwn survey tool type: %s", surveyTool);
			case "all":
				return GalacticResourceType.RESOURCE;
		}
	}
	
}
