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
package com.projectswg.holocore.services.support.data;

import com.projectswg.common.data.encodables.galaxy.Galaxy.GalaxyStatus;
import com.projectswg.common.network.packets.swg.admin.AdminShutdownServer;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.data.control.ServerManagementIntent;
import com.projectswg.holocore.intents.support.data.control.ServerStatusIntent;
import com.projectswg.holocore.resources.support.data.control.ServerStatus;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ServerStatusService extends Service {
	
	private static final TimeUnit [] TIME_SEARCH_PRIORITY = new TimeUnit[] {TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS};
	private static final long [] DISPLAY_COUNTERS = new long[] {1, 2, 3, 4, 5, 10, 15, 30, 45};
	private static final String SHUTDOWN_PERIODIC = "The server will be shutting down in %d %s.";
	private static final String SHUTDOWN_MESSAGE = "The server will now be shutting down.";
	
	private final ScheduledThreadPool executor;
	private final AtomicLong shutdownTime;
	private final AtomicBoolean operational;
	
	public ServerStatusService() {
		this.executor = new ScheduledThreadPool(1, Thread.MAX_PRIORITY, "shutdown-service");
		this.shutdownTime = new AtomicLong(0);
		this.operational = new AtomicBoolean(true);
	}
	
	@Override
	public boolean initialize() {
		executor.start();
		return true;
	}
	
	@Override
	public boolean isOperational() {
		return operational.get();
	}
	
	@Override
	public boolean terminate() {
		executor.stop();
		executor.awaitTermination(1000);
		return super.terminate();
	}
	
	@IntentHandler
	private void handleServerStatusIntent(ServerStatusIntent ssi) {
		switch (ssi.getStatus()) {
			case OFFLINE:
				ProjectSWG.getGalaxy().setStatus(GalaxyStatus.DOWN);
				break;
			case INITIALIZING:
				ProjectSWG.getGalaxy().setStatus(GalaxyStatus.LOADING);
				break;
			case OPEN:
				ProjectSWG.getGalaxy().setStatus(GalaxyStatus.UP);
				break;
			case SHUTDOWN_REQUESTED:
				startShutdownClock(ssi.getTime(), ssi.getTimeUnit());
				break;
			case LOCKED:
			case TERMINATING:
				ProjectSWG.getGalaxy().setStatus(GalaxyStatus.LOCKED);
				break;
		}
	}
	
	@IntentHandler
	private void handleServerManagementIntent(ServerManagementIntent smi) {
		switch (smi.getEvent()) {
			case SHUTDOWN:
				initiateShutdownSequence(smi.getTime(), smi.getTimeUnit());
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent ipi) {
		if (ipi.getPacket() instanceof AdminShutdownServer)
			initiateShutdownSequence(((AdminShutdownServer) ipi.getPacket()).getShutdownTime(), TimeUnit.SECONDS);
	}
	
	private void initiateShutdownSequence(long time, TimeUnit unit) {
		Log.i("Beginning server shutdown sequence...");
		
		new ServerStatusIntent(ServerStatus.SHUTDOWN_REQUESTED, time, unit).broadcast();
	}
	
	private void startShutdownClock(long time, TimeUnit unit) {
		shutdownTime.set(System.nanoTime() + unit.toNanos(time));
		executor.executeWithFixedRate(0, 1000, this::updateShutdownClock);
	}
	
	private void updateShutdownClock() {
		long shutdownTime = this.shutdownTime.get();
		long currentTime = System.nanoTime();
		long remainingSeconds = (long) ((shutdownTime - currentTime) / 1E9);
		assert shutdownTime != 0;
		if (currentTime >= shutdownTime) { // time's up
			operational.set(false);
		} else if (remainingSeconds < 2) {
			SystemMessageIntent.broadcastGalaxy(SHUTDOWN_MESSAGE);
		} else {
			String message = buildShutdownMessage(remainingSeconds);
			if (message != null)
				SystemMessageIntent.broadcastGalaxy(message);
		}
	}
	
	private static String buildShutdownMessage(long timeRemainingSeconds) {
		TimeUnit unit = getAppropriateTimeUnit(timeRemainingSeconds);
		if (unit == null)
			return null;
		long remaining = TimeUnit.SECONDS.convert(timeRemainingSeconds, unit);
		String units = unit.name().toLowerCase(Locale.US);
		if (remaining == 1)
			units = units.substring(0, units.length()-1); // drop the 's'
		return String.format(SHUTDOWN_PERIODIC, remaining, units);
	}
	
	private static TimeUnit getAppropriateTimeUnit(long timeRemainingSeconds) {
		for (TimeUnit unit : TIME_SEARCH_PRIORITY) {
			for (long count : DISPLAY_COUNTERS) {
				if (unit.toSeconds(count) == timeRemainingSeconds)
					return unit;
			}
		}
		return null;
	}
	
}
