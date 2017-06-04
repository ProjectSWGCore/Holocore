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
package resources.objects.custom;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import resources.objects.creature.CreatureObject;
import utilities.ScheduledUtilities;

public abstract class AIObject extends CreatureObject {
	
	private transient ScheduledFuture<?> future;
	private long initialDelay;
	private long delay;
	private TimeUnit unit;
	private String creatureId;
	
	public AIObject(long objectId) {
		super(objectId);
		aiInitialize();
	}
	
	/**
	 * Called upon object creation.  If overridden, you must call this
	 * function via super.aiInitialize()
	 */
	protected void aiInitialize() {
		setSchedulerProperties(0, 5, TimeUnit.SECONDS);
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
	
	/**
	 * Called when Holocore is starting.  If overridden, you must call this
	 * function via super.aiStart()
	 */
	public void aiStart() {
		if (future != null) {
			return;
		}
		future = ScheduledUtilities.scheduleAtFixedRate(() -> aiLoop(), initialDelay, delay, unit);
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
}
