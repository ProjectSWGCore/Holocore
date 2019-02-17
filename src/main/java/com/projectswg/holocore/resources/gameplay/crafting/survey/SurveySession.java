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
package com.projectswg.holocore.resources.gameplay.crafting.survey;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.common.network.packets.swg.zone.crafting.surveying.SurveyMessage;
import com.projectswg.common.network.packets.swg.zone.crafting.surveying.SurveyMessage.ResourceConcentration;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawn;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.utilities.ScheduledUtilities;
import me.joshlarson.jlcommon.log.Log;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;

public class SurveySession {
	
	private final CreatureObject creature;
	private final TangibleObject surveyTool;
	private final GalacticResource resource;
	private final AtomicReference<ScheduledFuture<?>> surveyRequest;
	
	public SurveySession(CreatureObject creature, TangibleObject surveyTool, GalacticResource resource) {
		this.creature = creature;
		this.surveyTool = surveyTool;
		this.resource = resource;
		this.surveyRequest = new AtomicReference<>(null);
	}
	
	public GalacticResource getResource() {
		return resource;
	}
	
	public synchronized void startSession() {
		ScheduledFuture<?> prev = surveyRequest.get();
		if (prev != null && !prev.isDone())
			return;
		
		surveyRequest.set(ScheduledUtilities.run(this::performSurvey, 4, SECONDS));
		creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("survey", "start_survey"), "TO", resource.getName())));
		creature.sendSelf(new PlayMusicMessage(0, getMusicFile(), 1, false));
		creature.sendObservers(new PlayClientEffectObjectMessage(getEffectFile(), "", creature.getObjectId(), ""));
	}
	
	public synchronized void stopSession() {
		ScheduledFuture<?> surveyRequest = this.surveyRequest.get();
		if (surveyRequest != null)
			surveyRequest.cancel(false);
	}
	
	private void performSurvey() {
		// Verify that we are able to survey
		switch (creature.getPosture()) {
			case SITTING:
				sendErrorMessage(creature, "survey_sitting");
				return;
			case UPRIGHT:
				break;
			default:
				sendErrorMessage(creature, "survey_standing");
				break;
		}
		if (creature.getInstanceLocation().getInstanceNumber() != 0) {
			sendErrorMessage(creature, "no_survey_instance");
			return;
		}
		if (creature.getParent() != null) {
			if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT))
				sendErrorMessage(creature, "survey_on_mount");
			else
				sendErrorMessage(creature, "survey_in_structure");
			return;
		}
		SurveyToolResolution resolution = getCurrentResolution();
		if (resolution == null) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "No survey tool resolution has been set"));
			return;
		}
		
		sendSurveyMessage(resolution);
		surveyRequest.set(null);
	}
	
	private void sendSurveyMessage(SurveyToolResolution resolution) {
		final double baseLocationX = creature.getX();
		final double baseLocationZ = creature.getZ();
		final double rangeHalf = resolution.getRange()/2.0;
		final double rangeInc = resolution.getRange()/(resolution.getResolution()-1.0);
		
		SurveyMessage surveyMessage = new SurveyMessage();
		List<GalacticResourceSpawn> spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, creature.getTerrain());
		for (double x = baseLocationX - rangeHalf, xIndex = 0; xIndex < resolution.getResolution(); x += rangeInc, xIndex++) {
			for (double z = baseLocationZ - rangeHalf, zIndex = 0; zIndex < resolution.getResolution(); z += rangeInc, zIndex++) {
				surveyMessage.addConcentration(new ResourceConcentration(x, z, getConcentration(spawns, creature.getTerrain(), x, z)));
			}
		}
		creature.sendSelf(surveyMessage);
	}
	
	private SurveyToolResolution getCurrentResolution() {
		Integer counterSetting = (Integer) surveyTool.getServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE);
		if (counterSetting == null)
			return null; // Must be set before using - invariant enforced within ObjectSurveyToolRadial.java
		int counter = counterSetting;
		
		List<SurveyToolResolution> resolutions = SurveyToolResolution.getOptions(creature);
		for (SurveyToolResolution resolution : resolutions) {
			if (resolution.getCounter() == counter)
				return resolution;
		}
		
		// Attempted to set a resolution that wasn't valid
		return resolutions.isEmpty() ? null : resolutions.get(resolutions.size()-1);
	}
	
	private double getConcentration(List<GalacticResourceSpawn> spawns, Terrain terrain, double x, double z) {
		int concentration = 0;
		for (GalacticResourceSpawn spawn : spawns) {
			concentration += spawn.getConcentration(terrain, x, z);
		}
		if (concentration < 10) // Minimum density
			return 0;
		return concentration / 100.0;
	}
	
	private String getMusicFile() {
		RawResource rawResource = resource.getRawResource();
		if (RawResourceType.MINERAL.isResourceType(rawResource))
			return "sound/item_mineral_tool_survey.snd";
		if (RawResourceType.WATER.isResourceType(rawResource))
			return "sound/item_moisture_tool_survey.snd";
		if (RawResourceType.CHEMICAL.isResourceType(rawResource))
			return "sound/item_liquid_tool_survey.snd";
		if (RawResourceType.FLORA_STRUCTURAL.isResourceType(rawResource))
			return "sound/item_lumber_tool_survey.snd";
		if (RawResourceType.GAS.isResourceType(rawResource))
			return "sound/item_gas_tool_survey.snd";
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource))
			return "sound/item_moisture_tool_survey.snd";
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource))
			return "sound/item_moisture_tool_survey.snd";
		Log.w("Unknown raw resource survey music file: %s with type %s", rawResource, rawResource.getResourceType());
		return "";
	}
	
	private String getEffectFile() {
		RawResource rawResource = resource.getRawResource();
		if (RawResourceType.MINERAL.isResourceType(rawResource))
			return "clienteffect/survey_tool_mineral.cef";
		if (RawResourceType.WATER.isResourceType(rawResource))
			return "clienteffect/survey_tool_moisture.cef";
		if (RawResourceType.CHEMICAL.isResourceType(rawResource))
			return "clienteffect/survey_tool_liquid.cef";
		if (RawResourceType.FLORA_STRUCTURAL.isResourceType(rawResource))
			return "clienteffect/survey_tool_lumber.cef";
		if (RawResourceType.GAS.isResourceType(rawResource))
			return "clienteffect/survey_tool_gas.cef";
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource))
			return "clienteffect/survey_tool_moisture.cef";
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource))
			return "clienteffect/survey_tool_moisture.cef";
		Log.w("Unknown raw resource survey effect file: %s with type %s", rawResource, rawResource.getResourceType());
		return "";
	}
	
	private static void sendErrorMessage(CreatureObject creature, String key) {
		creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("error_message", key))));
	}
	
}
