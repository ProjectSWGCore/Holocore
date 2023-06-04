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

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawn;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import com.projectswg.holocore.services.gameplay.crafting.resource.ResourceContainerEventHandler;
import com.projectswg.holocore.services.gameplay.crafting.resource.ResourceContainerHelper;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

public class SampleLoopSession {

	private final @NotNull CreatureObject creature;
	private final @NotNull TangibleObject surveyTool;
	private final @NotNull GalacticResource resource;
	private final @NotNull Location sampleLocation;

	private SuiWindow sampleWindow;
	private ScheduledFuture<?> loopFuture;
	private boolean paused;
	private int sampleMultiplier;

	public SampleLoopSession(@NotNull CreatureObject creature, @NotNull TangibleObject surveyTool, @NotNull GalacticResource resource, @NotNull Location sampleLocation) {
		this.creature = creature;
		this.surveyTool = surveyTool;
		this.resource = resource;
		this.sampleLocation = sampleLocation;

		this.sampleWindow = null;
		this.loopFuture = null;
		this.paused = false;
		this.sampleMultiplier = 1;
	}

	public boolean isMatching(CreatureObject creature, TangibleObject surveyTool, GalacticResource resource, Location sampleLocation) {
		if (this.sampleLocation.distanceTo(sampleLocation) >= 0.5 || this.sampleLocation.getTerrain() != sampleLocation.getTerrain())
			return false; // Too far away for the same session
		return this.creature.equals(creature) && this.surveyTool.equals(surveyTool) && this.resource.equals(resource);
	}

	public synchronized boolean isSampling() {
		return loopFuture != null && !loopFuture.isDone();
	}

	public synchronized void onPlayerMoved() {
		Location oldLocation = sampleLocation;
		Location newLocation = creature.getWorldLocation();

		if (oldLocation.distanceTo(newLocation) >= 0.5 || oldLocation.getTerrain() != newLocation.getTerrain())
			stopSession();
	}

	/**
	 * Attempts to start the sample loop session with the specified executor.  If the loop is unable to start, this function returns false; otherwise this function returns true.
	 *
	 * @param executor the executor for the loop to run on
	 * @return TRUE if the sample loop has begin, FALSE otherwise
	 */
	public synchronized boolean startSession(ScheduledThreadPool executor) {
		if (loopFuture != null) {
			loopFuture.cancel(false);
		}
		double concentration = getConcentration();
		if (!isAllowedToSample(concentration))
			return false;

		loopFuture = executor.executeWithFixedRate(30_000, 30_000, this::sample);
		creature.setPosture(Posture.CROUCHED);
		creature.setMovementPercent(0);
		creature.setTurnScale(0);
		creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:start_sampling"), "TO", resource.getName())));
		StandardLog.onPlayerTrace(this, creature, "started a sample session with %s and concentration %.1f", resource.getName(), concentration);
		return true;
	}

	public synchronized void stopSession() {
		if (loopFuture != null)
			loopFuture.cancel(false);
		else
			return;
		loopFuture = null;
		if (sampleWindow != null) {
			Player owner = creature.getOwner();
			if (owner != null)
				sampleWindow.close(owner);
		}
		sampleWindow = null;

		creature.setPosture(Posture.UPRIGHT);
		creature.setMovementPercent(1);
		creature.setTurnScale(1);
		creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_cancel"));
	}

	private void sample() {
		final double concentration = getConcentration();
		if (!isAllowedToSample(concentration) || paused)
			return;

		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (random.nextDouble() > concentration) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:sample_failed"), "TO", resource.getName())));
			sendSampleEffects();
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
			}
		}

		Player owner = creature.getOwner();
		if (owner != null) {
			ResourceContainerEventHandler resourceContainerEventHandler = new ResourceContainerEventHandler() {

				@Override
				public void onUnknownError() {
					creature.sendSelf(
							new ChatSystemMessage(
									SystemChatType.PERSONAL, "The was an unknown server error when transferring resources to your inventory"
							)
					);
				}

				@Override
				public void onInventoryFull() {
					creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:no_inv_space"));
					stopSession();
				}

				@Override
				public void onSuccess() {
					creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:sample_located"), "DI", resourceAmount, "TO", resource.getName())));
					creature.sendSelf(new PlayMusicMessage(0, "sound/item_internalstorage_open.snd", 1, false));
				}
			};
			ResourceContainerHelper.INSTANCE.giveResourcesToPlayer(resourceAmount, resource, owner, resourceContainerEventHandler);
			sendSampleEffects();
		}
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

	private void createHighestConcentrationWaypoint() {
		double highestX = sampleLocation.getX();
		double highestZ = sampleLocation.getZ();
		double highest = 0;

		for (double x = sampleLocation.getX() - 5; x < sampleLocation.getX() + 5; x += 1) {
			for (double z = sampleLocation.getZ() - 5; z < sampleLocation.getZ() + 5; z += 1) {
				List<GalacticResourceSpawn> spawns = resource.getSpawns(sampleLocation.getTerrain());
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

		creature.getPlayerObject()
				.getWaypoints()
				.entrySet()
				.stream()
				.filter(e -> "Resource Survey".equals(e.getValue().getName()))
				.filter(e -> e.getValue().getColor() == WaypointColor.ORANGE)
				.filter(e -> e.getValue().getTerrain() == sampleLocation.getTerrain())
				.map(Entry::getKey)
				.forEach(creature.getPlayerObject()::removeWaypoint);
		createResourceWaypoint(Location.builder(sampleLocation).setX(highestX).setZ(highestZ).build());
		creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:node_waypoint"));
	}

	private boolean isAllowedToSample(double concentration) {
		if (resource.getSpawns(creature.getTerrain()).isEmpty()) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_empty"));
			stopSession();
			return false;
		}
		if (!creature.getInventory().getContainedObjects().contains(surveyTool)) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_gone"));
			stopSession();
			return false;
		}
		if (creature.getSkillModValue("surveying") < 20) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "You aren't allowed to sample without a surveying skillmod"));
			return false;
		}
		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "You aren't allowed to sample while on a mount"));
			return false;
		}
		if (creature.getParent() != null) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "You aren't allowed to sample while within a building"));
			return false;
		}
		if (creature.isInCombat()) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_cancel_attack"));
			return false;
		}
		if (concentration <= 0.3) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:density_below_threshold"), "TO", resource.getName())));
			stopSession();
			return false;
		}
		return true;
	}

	private double getConcentration() {
		List<GalacticResourceSpawn> spawns = resource.getSpawns(creature.getTerrain());
		double concentration = 0;
		for (GalacticResourceSpawn spawn : spawns) {
			concentration += spawn.getConcentration(sampleLocation.getTerrain(), sampleLocation.getX(), sampleLocation.getZ()) / 100.0;
		}
		return concentration;
	}

	private void sendSampleEffects() {
		creature.sendSelf(new PlayMusicMessage(0, getMusicFile(), 1, false));
		creature.sendObservers(new PlayClientEffectObjectMessage(getEffectFile(), "", creature.getObjectId(), ""));
	}

	private String getMusicFile() {
		RawResource rawResource = ServerData.INSTANCE.getRawResources().getResource(resource.getRawResourceId());
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
		RawResource rawResource = ServerData.INSTANCE.getRawResources().getResource(resource.getRawResourceId());
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
