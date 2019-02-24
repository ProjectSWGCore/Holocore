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
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawn;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceStats;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.permissions.ContainerResult;
import com.projectswg.holocore.resources.support.objects.permissions.ReadWritePermissions;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.resource.ResourceContainerObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.log.Log;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class SampleHandler {
	
	private final CreatureObject creature;
	private final AtomicReference<Location> sampleLocation;
	private final AtomicReference<ScheduledFuture<?>> sampleLoop;
	private final ScheduledThreadPool executor;
	
	public SampleHandler(CreatureObject creature, ScheduledThreadPool executor) {
		this.creature = creature;
		this.sampleLocation = new AtomicReference<>(null);
		this.sampleLoop = new AtomicReference<>(null);
		this.executor = executor;
	}
	
	public void startSession() {
		
	}
	
	public void stopSession() {
		stopSampleLoop();
	}
	
	public void startSampleLoop(GalacticResource resource) {
		Location location = creature.getWorldLocation();
		double concentration = getConcentration(resource, location);
		
		sampleLocation.set(location);
		if (!isAllowedToSample(resource, concentration))
			return;
		
		creature.setPosture(Posture.CROUCHED);
		creature.setMovementPercent(0);
		creature.setTurnScale(0);
		creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:start_sampling"), "TO", resource.getName())));
		creature.sendSelf(new PlayMusicMessage(0, getMusicFile(resource), 1, false));
		creature.sendObservers(new PlayClientEffectObjectMessage(getEffectFile(resource), "", creature.getObjectId(), ""));
		Log.d("%s started a sample session with %s and concentration %.1f", creature.getObjectName(), resource.getName(), concentration);
		
		sampleLoop.set(executor.executeWithFixedRate(5000, 5000, () -> sample(resource, location)));
	}
	
	public void onPlayerMoved() {
		Location oldLocation = sampleLocation.get();
		Location newLocation = creature.getWorldLocation();
		
		if (oldLocation == null)
			return;
		if (oldLocation.distanceTo(newLocation) >= 0.5 || oldLocation.getTerrain() != newLocation.getTerrain())
			stopSampleLoop();
	}
	
	public void stopSampleLoop() {
		ScheduledFuture<?> loop = this.sampleLoop.getAndSet(null);
		if (loop != null && loop.cancel(false)) {
			creature.setPosture(Posture.UPRIGHT);
			creature.setMovementPercent(1);
			creature.setTurnScale(1);
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_cancel"));
		}
	}
	
	public boolean isSampling() {
		ScheduledFuture<?> loop = this.sampleLoop.get();
		return loop != null && !loop.isDone();
	}
	
	private void sample(GalacticResource resource, Location location) {
		double concentration = getConcentration(resource, location);
		if (concentration <= 0.3) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:density_below_threshold"), "TO", resource.getName())));
			return;
		}
		
		int resourceAmount = ThreadLocalRandom.current().nextInt(19, 25);
		ResourceContainerObject resourceObject = createResourceObject(resource, resourceAmount);
		ContainerResult result = resourceObject.moveToContainer(creature, creature.getInventory());
		switch (result) {
			case SLOT_OCCUPIED:
			case SLOT_NO_EXIST:
			case NO_PERMISSION:
				creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "The was an unknown server error when transferring resources to your inventory"));
				IntentChain.broadcastChain(new ObjectCreatedIntent(resourceObject), new DestroyObjectIntent(resourceObject));
				break;
			case CONTAINER_FULL:
				creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:no_inv_space"));
				IntentChain.broadcastChain(new ObjectCreatedIntent(resourceObject), new DestroyObjectIntent(resourceObject));
				stopSampleLoop();
				break;
			case SUCCESS:
				creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:sample_located"), "DI", resourceAmount, "TO", resource.getName())));
				ObjectCreatedIntent.broadcast(resourceObject);
				break;
		}
	}
	
	private boolean isAllowedToSample(GalacticResource resource, double concentration) {
		if (creature.getSkillModValue("surveying") < 20) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "You aren't allowed to sample without a surveying skillmod"));
			return false;
		}
		if (creature.isInCombat()) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_cancel_attack"));
			return false;
		}
		if (concentration <= 0.3) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:density_below_threshold"), "TO", resource.getName())));
			return false;
		}
		return true;
	}
	
	private double getConcentration(GalacticResource resource, Location location) {
		List<GalacticResourceSpawn> spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, creature.getTerrain());
		double concentration = 0;
		for (GalacticResourceSpawn spawn : spawns) {
			concentration += spawn.getConcentration(location.getTerrain(), location.getX(), location.getZ()) / 100.0;
		}
		return concentration;
	}
	
	private ResourceContainerObject createResourceObject(GalacticResource resource, int amount) {
		ResourceContainerObject resourceObject = (ResourceContainerObject) ObjectCreator.createObjectFromTemplate(resource.getRawResource().getCrateTemplate());
		resourceObject.setQuantity(amount);
		resourceObject.setParentName(resource.getRawResource().getParent().getName().toString());
		resourceObject.setResourceType(resource.getRawResourceId());
		resourceObject.setResourceName(resource.getName());
		assignStats(resourceObject, resource);
		resourceObject.setContainerPermissions(ReadWritePermissions.from(creature));
		return resourceObject;
	}
	
	private static void assignStats(TangibleObject obj, GalacticResource resource) {
		GalacticResourceStats stats = resource.getStats();
		obj.setServerAttribute(ServerAttribute.GALACTIC_RESOURCE_ID, resource.getId());
		transferStat(obj, "res_cold_resist", stats.getColdResistance());
		transferStat(obj, "res_conductivity", stats.getConductivity());
		transferStat(obj, "res_decay_resist", stats.getDecayResistance());
		transferStat(obj, "entangle_resistance", stats.getEntangleResistance());
		transferStat(obj, "res_flavor", stats.getFlavor());
		transferStat(obj, "res_heat_resist", stats.getHeatResistance());
		transferStat(obj, "res_malleability", stats.getMalleability());
		transferStat(obj, "res_quality", stats.getOverallQuality());
		transferStat(obj, "res_potential_energy", stats.getPotentialEnergy());
		transferStat(obj, "res_shock_resistance", stats.getShockResistance());
		transferStat(obj, "res_toughness", stats.getUnitToughness());
	}
	
	private static void transferStat(TangibleObject obj, String name, int attr) {
		obj.addAttribute("@obj_attr_n:"+name, String.valueOf(attr));
	}
	
	private static String getMusicFile(GalacticResource resource) {
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
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource))
			return "sound/item_moisture_tool_sample.snd";
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource))
			return "sound/item_moisture_tool_sample.snd";
		Log.w("Unknown raw resource sample music file: %s with type %s", rawResource, rawResource.getResourceType());
		return "";
	}
	
	private static String getEffectFile(GalacticResource resource) {
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
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource))
			return "clienteffect/survey_sample_moisture.cef";
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource))
			return "clienteffect/survey_sample_moisture.cef";
		Log.w("Unknown raw resource sample effect file: %s with type %s", rawResource, rawResource.getResourceType());
		return "";
	}
	
}
