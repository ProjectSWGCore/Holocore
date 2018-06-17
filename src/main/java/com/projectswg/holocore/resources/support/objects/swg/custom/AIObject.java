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
package com.projectswg.holocore.resources.support.objects.swg.custom;

import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.npc.ai.AICombatSupport;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;

public abstract class AIObject extends CreatureObject {
	
	private final Set<CreatureObject> playersNearby;
	private final List<ScheduledFuture<?>> scheduledTasks;
	private final AICombatSupport combatSupport;
	
	private ScheduledThreadPool executor;
	private ScheduledMode mode;
	private String creatureId;
	
	public AIObject(long objectId) {
		super(objectId);
		this.playersNearby = new CopyOnWriteArraySet<>();
		this.scheduledTasks = new ArrayList<>();
		this.combatSupport = new AICombatSupport(this);
		
		this.executor = null;
		this.mode = null;
		this.creatureId = null;
		setRunSpeed(7.3);
	}
	
	@Override
	public void onObjectMoveInAware(SWGObject aware) {
		if (aware.getBaselineType() != BaselineType.CREO || !((CreatureObject) aware).isLoggedInPlayer())
			return;
		if (getLocation().distanceTo(aware.getLocation()) <= 300) {
			playersNearby.add((CreatureObject) aware);
		} else {
			playersNearby.remove(aware);
		}
	}
	
	public String getCreatureId() {
		return creatureId;
	}
	
	public void setCreatureId(String creatureId) {
		this.creatureId = creatureId;
	}
	
	public final synchronized void scheduleDefaultMode(ScheduledThreadPool executor) {
		if (mode == ScheduledMode.DEFAULT)
			return;
		mode = ScheduledMode.DEFAULT;
		this.executor = executor;
		
		disableScheduler();
		scheduledTasks.add(executor.executeWithFixedRate(0, getDefaultModeInterval(), this::defaultModeLoop));
	}
	
	public final synchronized void scheduleCombatMode(ScheduledThreadPool executor) {
		if (mode == ScheduledMode.COMBAT)
			return;
		mode = ScheduledMode.COMBAT;
		this.executor = executor;
		
		disableScheduler();
		scheduledTasks.add(executor.executeWithFixedRate(0, 500, this::combatModeLoop));
	}
	
	public final synchronized void disableScheduler() {
		scheduledTasks.forEach(sf -> sf.cancel(false));
		scheduledTasks.clear();
	}
	
	public double calculateWalkSpeed() {
		return getMovementPercent() * getMovementScale() * getWalkSpeed();
	}
	
	public double calculateRunSpeed() {
		return getMovementPercent() * getMovementScale() * getRunSpeed();
	}
	
	protected final boolean isRooted() {
		switch (getPosture()) {
			case DEAD:
			case INCAPACITATED:
			case INVALID:
			case KNOCKED_DOWN:
			case LYING_DOWN:
			case SITTING:
				return true;
			case BLOCKING:
			case CLIMBING:
			case CROUCHED:
			case DRIVING_VEHICLE:
			case FLYING:
			case PRONE:
			case RIDING_CREATURE:
			case SKILL_ANIMATING:
			case SNEAKING:
			case UPRIGHT:
			default:
				// Rooted if there are no nearby players
				return playersNearby.isEmpty();
		}
	}
	
	protected final Set<CreatureObject> getNearbyPlayers() {
		return Collections.unmodifiableSet(playersNearby);
	}
	
	protected abstract long getDefaultModeInterval();
	
	protected abstract void defaultModeLoop();
	
	private void combatModeLoop() {
		combatSupport.act();
		if (!combatSupport.isExecuting()) {
			combatSupport.reset();
			scheduleDefaultMode(executor);
		}
	}
	
	private enum ScheduledMode {
		DEFAULT,
		COMBAT
	}
	
}
