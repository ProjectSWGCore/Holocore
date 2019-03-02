/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawn;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceStats;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.CollectionLoader.CollectionSlotInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.network.DisconnectReason;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.permissions.ContainerResult;
import com.projectswg.holocore.resources.support.objects.permissions.ReadWritePermissions;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.resource.ResourceContainerObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.log.Log;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

public class SampleLoopSession {
	
	private final CreatureObject creature;
	private final TangibleObject surveyTool;
	private final GalacticResource resource;
	
	private SuiWindow sampleWindow;
	private Location sampleLocation;
	private ScheduledFuture<?> loopFuture;
	private boolean paused;
	private int sampleMultiplier;
	
	public SampleLoopSession(CreatureObject creature, TangibleObject surveyTool, GalacticResource resource) {
		this.creature = creature;
		this.surveyTool = surveyTool;
		this.resource = resource;
		
		this.sampleWindow = null;
		this.sampleLocation = null;
		this.loopFuture = null;
		this.paused = false;
		this.sampleMultiplier = 1;
	}
	
	public synchronized void startSession(ScheduledThreadPool executor, GalacticResource resource, Location sampleLocation) {
		this.sampleLocation = sampleLocation;
		if (loopFuture != null) {
			loopFuture.cancel(false);
		}
		loopFuture = executor.executeWithFixedRate(5000, 5000, this::sample);
	}
	
	public synchronized void stopSession() {
		if (loopFuture != null)
			loopFuture.cancel(false);
		loopFuture = null;
	}
	
	private synchronized void sample() {
		if (GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, creature.getTerrain()).isEmpty()) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_empty"));
			stopSession();
			return;
		}
		if (!creature.getInventory().getContainedObjects().contains(surveyTool)) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_gone"));
			stopSession();
			return;
		}
		double concentration = getConcentration();
		if (concentration <= 0.3) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:density_below_threshold"), "TO", resource.getName())));
			stopSession();
			return;
		}
		if (paused)
			return;
		
		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (random.nextDouble() > concentration) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:sample_failed"), "TO", resource.getName())));
			return;
		}
		
		int resourceAmount = random.nextInt(19, 25) * sampleMultiplier;
		if (random.nextDouble() < 0.1) {
			double roll = random.nextDouble();
			if (roll < 0.5) { // 0.50
				creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:critical_success"));
				sampleMultiplier *= 2;
			} else if (roll < 0.75) { // 0.75
				openConcentrationWindow();
			} else {
				openGambleWindow();
			}
		}
		
		ResourceContainerObject resourceObject = createResourceObject(resourceAmount);
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
				stopSession();
				break;
			case SUCCESS:
				creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:sample_located"), "DI", resourceAmount, "TO", resource.getName())));
				creature.sendSelf(new PlayMusicMessage(0, "sound/item_internalstorage_open.snd", 1, false));
				ObjectCreatedIntent.broadcast(resourceObject);
				break;
		}
		creature.sendSelf(new PlayMusicMessage(0, getMusicFile(), 1, false));
		creature.sendObservers(new PlayClientEffectObjectMessage(getEffectFile(), "", creature.getObjectId(), ""));
	}
	
	private void openConcentrationWindow() {
		SuiMessageBox window = new SuiMessageBox(SuiButtons.OK_CANCEL, "@survey:cnode_t", "@survey:cnode_d");
		window.setProperty("btnOk", "Text", "@survey:cnode_2");
		window.setProperty("btnCancel", "Text", "@survey:cnode_1");
		window.addOkButtonCallback("okButton", (event, parameters) -> {
			int index = SuiListBox.getSelectedRow(parameters);
			this.sampleWindow = null;
			if (index == 0) {
				createHighestConcentrationWaypoint();
				stopSession();
			}
		});
		window.addCancelButtonCallback("cancelButton", (event, parameters) -> this.sampleWindow = null);
		if (this.sampleWindow == null) {
			this.sampleWindow = window;
			window.display(creature.getOwner());
		}
		paused = true;
	}
	
	private void openGambleWindow() {
		CollectionSlotInfo collection = DataLoader.collections().getSlotByName("col_resource_" + surveyTool.getTemplate().substring(47, surveyTool.getTemplate().length() - 4) + "_01");
		boolean hasCollectionListItem = collection != null && !creature.getPlayerObject().getCollectionFlag(collection.getBeginSlotId());
		SuiListBox window = new SuiListBox(SuiButtons.OK_CANCEL, "@survey:gnode_t", "@survey:gnode_d");
		window.addListItem("@survey:gnode_1");
		window.addListItem("@survey:gnode_2");
		if (hasCollectionListItem) {
			window.addListItem("@survey:gnode_collection");
		}
		
		window.addOkButtonCallback("okButton", (event, parameters) -> {
			int index = SuiListBox.getSelectedRow(parameters);
			this.sampleWindow = null;
			if (index == 0) {
				creature.modifyAction(-2000);
				if (ThreadLocalRandom.current().nextDouble() < 0.75) {
					sampleMultiplier *= 4;
				}
			} else if (index == 2) {
				if (!hasCollectionListItem) {
					StandardLog.onPlayerError(this, creature, "Attempted to select an inaccessible sampling list item");
					Player owner = creature.getOwner();
					if (owner != null)
						CloseConnectionIntent.broadcast(owner, DisconnectReason.SUSPECTED_HACK);
					return;
				}
				// TODO: Add collection
			}
			stopSession();
		});
		window.addCancelButtonCallback("cancelButton", (event, parameters) -> this.sampleWindow = null);
		if (this.sampleWindow == null) {
			this.sampleWindow = window;
			window.display(creature.getOwner());
		}
		paused = true;
	}
	
	private void createHighestConcentrationWaypoint() {
		if (sampleLocation == null)
			return;
		
		double highestX = sampleLocation.getX();
		double highestZ = sampleLocation.getZ();
		double highest = 0;
		
		for (double x = sampleLocation.getX() - 5; x < sampleLocation.getX() + 5; x += 1) {
			for (double z = sampleLocation.getZ() - 5; z < sampleLocation.getZ() + 5; z += 1) {
				List<GalacticResourceSpawn> spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, sampleLocation.getTerrain());
				double concentration = 0;
				for (GalacticResourceSpawn spawn : spawns) {
					concentration += spawn.getConcentration(sampleLocation.getTerrain(), x, z);
				}
				if (concentration > highest) {
					highestX = x;
					highestZ = z;
					highest = concentration;
				}
			}
		}
		
		creature.getPlayerObject().getWaypoints().entrySet().stream().filter(e -> "Resource Survey".equals(e.getValue().getName())).filter(e -> e.getValue().getColor() == WaypointColor.ORANGE)
				.filter(e -> e.getValue().getTerrain() == sampleLocation.getTerrain()).map(Entry::getKey).forEach(creature.getPlayerObject()::removeWaypoint);
		createResourceWaypoint(Location.builder(sampleLocation).setX(highestX).setZ(highestZ).build());
		creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:node_waypoint"));
	}
	
	private double getConcentration() {
		List<GalacticResourceSpawn> spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, creature.getTerrain());
		double concentration = 0;
		for (GalacticResourceSpawn spawn : spawns) {
			concentration += spawn.getConcentration(sampleLocation.getTerrain(), sampleLocation.getX(), sampleLocation.getZ()) / 100.0;
		}
		return concentration;
	}
	
	private ResourceContainerObject createResourceObject(int amount) {
		ResourceContainerObject resourceObject = (ResourceContainerObject) ObjectCreator.createObjectFromTemplate(resource.getRawResource().getCrateTemplate());
		resourceObject.setQuantity(amount);
		resourceObject.setParentName(resource.getRawResource().getParent().getName().toString());
		resourceObject.setResourceType(resource.getRawResourceId());
		resourceObject.setResourceName(resource.getName());
		resourceObject.setObjectName(resource.getName());
		assignStats(resourceObject);
		resourceObject.setContainerPermissions(ReadWritePermissions.from(creature));
		return resourceObject;
	}
	
	private void assignStats(TangibleObject obj) {
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
		obj.addAttribute("@obj_attr_n:" + name, String.valueOf(attr));
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
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource))
			return "sound/item_moisture_tool_sample.snd";
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource))
			return "sound/item_moisture_tool_sample.snd";
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
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource))
			return "clienteffect/survey_sample_moisture.cef";
		if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource))
			return "clienteffect/survey_sample_moisture.cef";
		Log.w("Unknown raw resource sample effect file: %s with type %s", rawResource, rawResource.getResourceType());
		return "";
	}
	
	private void createResourceWaypoint(Location location) {
		WaypointObject waypoint = (WaypointObject) ObjectCreator.createObjectFromTemplate("object/waypoint/shared_waypoint.iff");
		waypoint.setPosition(location.getTerrain(), location.getX(), location.getY(), location.getZ());
		waypoint.setColor(WaypointColor.ORANGE);
		waypoint.setName("Resource Survey");
		ObjectCreatedIntent.broadcast(waypoint);
		creature.getPlayerObject().addWaypoint(waypoint);
	}
	
}
