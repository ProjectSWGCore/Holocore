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
package com.projectswg.holocore.resources.objects.custom;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.utilities.ScheduledUtilities;

public abstract class AIObject extends CreatureObject {
	
	private final Set<CreatureObject> playersNearby;
	
	private transient ScheduledFuture<?> future;
	private long initialDelay;
	private long delay;
	private TimeUnit unit;
	private String creatureId;
	
	public AIObject(long objectId) {
		super(objectId);
		this.playersNearby = new CopyOnWriteArraySet<>();
		aiInitialize();
	}
	
	/**
	 * Called upon object creation.  If overridden, you must call this
	 * function via super.aiInitialize()
	 */
	protected void aiInitialize() {
		setSchedulerProperties(0, 5, TimeUnit.SECONDS);
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
	
	/**
	 * Sets scheduler properties for how often aiLoop runs
	 * @param initialDelay the initial delay
	 * @param delay the delay between each loop
	 * @param unit the time unit for both delays
	 */
	protected void setSchedulerProperties(long initialDelay, long delay, TimeUnit unit) {
		this.initialDelay = initialDelay;
		this.delay = delay;
		this.unit = unit;
	}
	
	protected void disableScheduler() {
		setSchedulerProperties(0, 0, null);
	}
	
	protected void requestNextLoop(long delay, TimeUnit unit) {
		ScheduledUtilities.run(this::aiLoop, delay, unit);
	}
	
	public void setSpeed(double speed) {
		setWalkSpeed(speed);
		setRunSpeed(speed);
	}
	
	/**
	 * Called when Holocore is starting.  If overridden, you must call this
	 * function via super.aiStart()
	 */
	public void aiStart() {
		if (future != null) {
			return;
		}
		if (unit != null)
			future = ScheduledUtilities.scheduleAtFixedRate(this::aiLoop, initialDelay, delay, unit);
	}
	
	/**
	 * Called periodically for move updates, etc.
	 */
	protected abstract void aiLoop();
	
	/**
	 * Called when Holocore is stopping.  If overridden, you must call this
	 * function via super.aiStop()
	 */
	public void aiStop() {
		if (future == null) {
			return;
		}
		future.cancel(true);
		future = null;
	}

	public String getCreatureId() {
		return creatureId;
	}

	public void setCreatureId(String creatureId) {
		this.creatureId = creatureId;
	}
	
	protected final boolean canAiMove() {
		switch (getPosture()) {
			case DEAD:
			case INCAPACITATED:
			case INVALID:
			case KNOCKED_DOWN:
			case LYING_DOWN:
			case SITTING:
				return false;
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
				return true;
		}
		return true;
	}
	
	protected final boolean hasNearbyPlayers() {
		return !playersNearby.isEmpty();
	}
	
	protected final Set<CreatureObject> getNearbyPlayers() {
		return Collections.unmodifiableSet(playersNearby);
	}
	
}
