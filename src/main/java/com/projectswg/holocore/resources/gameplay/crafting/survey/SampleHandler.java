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
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.data.encodables.tangible.Posture;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SampleHandler {
	
	private final AtomicReference<Location> sampleLocation;
	private final AtomicReference<ScheduledFuture<?>> sampleLoop;
	private final CreatureObject creature;
	private final TangibleObject surveyTool;
	private final AtomicBoolean running;
	private final SampleLoopSession session;
	private final ScheduledThreadPool executor;
	
	public SampleHandler(CreatureObject creature, TangibleObject surveyTool, ScheduledThreadPool executor) {
		this.sampleLocation = new AtomicReference<>(null);
		this.sampleLoop = new AtomicReference<>(null);
		this.session = new AtomicReference<>(null);
		this.creature = creature;
		this.surveyTool = surveyTool;
		this.running = new AtomicBoolean(false);
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
		
		if (!isAllowedToSample(resource, concentration))
			return;
		
		if (!running.getAndSet(true)) {
			ScheduledFuture<?> prevLoop = sampleLoop.getAndSet(executor.executeWithFixedRate(5000, 5000, () -> sample(resource, location)));
			stopSampleLoop(prevLoop);
			sampleLocation.set(location);
			creature.setPosture(Posture.CROUCHED);
			creature.setMovementPercent(0);
			creature.setTurnScale(0);
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, new ProsePackage(new StringId("@survey:start_sampling"), "TO", resource.getName())));
			Log.d("%s started a sample session with %s and concentration %.1f", creature.getObjectName(), resource.getName(), concentration);
		}
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
		if (!running.getAndSet(false))
			return;
		stopSampleLoop(sampleLoop.getAndSet(null));
	}
	
	public boolean isSampling() {
		ScheduledFuture<?> loop = this.sampleLoop.get();
		return loop != null && !loop.isDone();
	}
	
	private void stopSampleLoop(ScheduledFuture<?> loop) {
		if (loop != null && loop.cancel(false)) {
			creature.setPosture(Posture.UPRIGHT);
			creature.setMovementPercent(1);
			creature.setTurnScale(1);
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_cancel"));
			SuiWindow window = this.sampleWindow.get();
			if (window != null)
				window.close(creature.getOwner());
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
	
}
