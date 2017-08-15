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

import java.util.List;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;

import resources.objects.creature.CreatureObject;
import services.crafting.resource.galactic.GalacticResource;
import services.crafting.resource.galactic.GalacticResourceSpawn;
import services.crafting.resource.galactic.RawResourceType;
import services.crafting.resource.galactic.storage.GalacticResourceContainer;
import services.crafting.resource.raw.RawResource;

public class SampleSession {
	
	private final CreatureObject creature;
	private final GalacticResource resource;
	
	public SampleSession(CreatureObject creature, GalacticResource resource) {
		this.creature = creature;
		this.resource = resource;
	}
	
	public GalacticResource getResource() {
		return resource;
	}
	
	public void startSession() {
		double concentration = getConcentration(creature);
		creature.setPosture(Posture.CROUCHED);
		creature.getOwner().sendPacket(new PlayMusicMessage(0, getMusicFile(), 1, false));
		creature.sendObserversAndSelf(new PlayClientEffectObjectMessage(getEffectFile(), "", creature.getObjectId(), ""));
		Log.d("%s started a sample session with %s and concentration %.1f", creature.getObjectName(), resource.getName(), concentration);
	}
	
	public void stopSession() {
		
	}
	
	private double getConcentration(CreatureObject creature) {
		List<GalacticResourceSpawn> spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, creature.getTerrain());
		return getConcentration(spawns, creature.getTerrain(), creature.getX(), creature.getZ());
	}
	
	private double getConcentration(List<GalacticResourceSpawn> spawns, Terrain terrain, double x, double z) {
		double concentration = 0;
		for (GalacticResourceSpawn spawn : spawns) {
			concentration += spawn.getConcentration(terrain, x, z) / 100.0;
		}
		return concentration;
	}
	
	private String getMusicFile() {
		RawResource rawResource = resource.getRawResource();
		if (RawResourceType.MINERAL.isResourceType(rawResource))
			return "sound/item_mineral_tool_sample.snd";
		if (RawResourceType.WATER.isResourceType(rawResource))
			return "sound/item_moisture_tool_sample.snd";
		if (RawResourceType.CHEMICAL.isResourceType(rawResource))
			return "sound/item_liquid_tool_sample.snd";
		if (RawResourceType.FLORA_STRUCTURAL.isResourceType(rawResource))
			return "sound/item_lumber_tool_sample.snd";
		if (RawResourceType.GAS.isResourceType(rawResource))
			return "sound/item_gas_tool_sample.snd";
		Log.w("Unknown raw resource sample music file: %s with type %s", rawResource, rawResource.getResourceType());
		return "";
	}
	
	private String getEffectFile() {
		RawResource rawResource = resource.getRawResource();
		if (RawResourceType.MINERAL.isResourceType(rawResource))
			return "clienteffect/survey_sample_mineral.cef";
		if (RawResourceType.WATER.isResourceType(rawResource))
			return "clienteffect/survey_sample_moisture.cef";
		if (RawResourceType.CHEMICAL.isResourceType(rawResource))
			return "clienteffect/survey_sample_liquid.cef";
		if (RawResourceType.FLORA_STRUCTURAL.isResourceType(rawResource))
			return "clienteffect/survey_sample_lumber.cef";
		if (RawResourceType.GAS.isResourceType(rawResource))
			return "clienteffect/survey_sample_gas.cef";
		Log.w("Unknown raw resource sample effect file: %s with type %s", rawResource, rawResource.getResourceType());
		return "";
	}
	
}
