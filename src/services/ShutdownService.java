/***********************************************************************************
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
package services;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.projectswg.common.control.Service;

import intents.chat.ChatBroadcastIntent;
import intents.chat.ChatBroadcastIntent.BroadcastType;
import intents.server.ServerStatusIntent;
import resources.control.ServerStatus;
import utilities.ThreadUtilities;

public class ShutdownService extends Service {
	
	private static final String SHUTDOWN_PERIODIC = "The server will be shutting down in %d %s.";
	private static final String SHUTDOWN_MESSAGE = "The server will now be shutting down.";
	private static final TimeUnit BROADCAST_UNIT = TimeUnit.NANOSECONDS;
	
	private final AtomicLong intervalCount;
	private final AtomicLong timeRemaining;
	private final AtomicInteger runCount;
	private ScheduledExecutorService shutdownExecutor;
	
	public ShutdownService() {
		intervalCount = new AtomicLong();
		runCount = new AtomicInteger();
		timeRemaining = new AtomicLong();
		shutdownExecutor = null;
		
		registerForIntent(ServerStatusIntent.class, ssi -> handleServerStatusIntent(ssi));
	}
	
	@Override
	public boolean terminate() {
		shutdownExecutor();
		return super.terminate();
	}
	
	private void handleServerStatusIntent(ServerStatusIntent ssi){
		if (ssi.getStatus() != ServerStatus.SHUTDOWN_REQUESTED)
			return;
		shutdown(ssi);
	}
	
	private void shutdown(ServerStatusIntent i) {
		final TimeUnit timeUnit = i.getTimeUnit();
		final long time = i.getTime();
		final long timeNanoSeconds = BROADCAST_UNIT.convert(time, timeUnit);
		timeRemaining.set(time);
		intervalCount.set((long) Math.ceil(Math.sqrt(Math.sqrt(timeNanoSeconds)) / 200));
		
		if (intervalCount.get() > timeNanoSeconds)
			intervalCount.set(time);
		
		if (time > 0)
			scheduleBroadcast(timeUnit, time, timeNanoSeconds / intervalCount.get());
		else
			broadcast(SHUTDOWN_MESSAGE);
	}
	
	private void scheduleBroadcast(final TimeUnit timeUnit, final long time, final long timeBetweenBroadcasts) {
		recreateExecutor();
		shutdownExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				String message;
				long timeRemainingVal = timeRemaining.get();
				
				if(runCount.getAndIncrement() == intervalCount.get()) {
					message = SHUTDOWN_MESSAGE;
				} else {
					String unitName = timeUnit.name().toLowerCase(Locale.ENGLISH);
					String units = unitName.substring(0, unitName.length()-1);
					if (timeRemainingVal > 1)
						units += "s";
					message = String.format(SHUTDOWN_PERIODIC, timeRemainingVal, units);
					timeRemaining.set(timeRemainingVal - timeUnit.convert(timeBetweenBroadcasts, BROADCAST_UNIT));
				}
				broadcast(message);
			}
		}, 0, timeBetweenBroadcasts, BROADCAST_UNIT);
	}
	
	private void recreateExecutor() {
		shutdownExecutor();
		shutdownExecutor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("shutdown-service"));
	}
	
	private void shutdownExecutor() {
		if (shutdownExecutor != null)
			shutdownExecutor.shutdownNow();
	}
	
	private void broadcast(String message) {
		new ChatBroadcastIntent(message, BroadcastType.GALAXY).broadcast();
	}
	
}
