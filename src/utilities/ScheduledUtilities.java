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
package utilities;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledUtilities {
	
	private static final Object mutex = new Object();
	private static ScheduledExecutorService executor;
	
	private ScheduledUtilities() {
		
	}
	
	private static ScheduledExecutorService getScheduler() {
		synchronized (mutex) {
			if (executor == null) {
				int processors = Runtime.getRuntime().availableProcessors();
				executor = Executors.newScheduledThreadPool(processors, ThreadUtilities.newThreadFactory("scheduled-utilities-%d"));
			}
			return executor;
		}
	}
	
	public static ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initialDelay, long period, TimeUnit unit) {
		return getScheduler().scheduleAtFixedRate(r, initialDelay, period, unit);
	}
	
	public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable r, long initialDelay, long delay, TimeUnit unit) {
		return getScheduler().scheduleWithFixedDelay(r, initialDelay, delay, unit);
	}
	
	public static ScheduledFuture<?> run(Runnable r, long delay, TimeUnit unit) {
		return getScheduler().schedule(r, delay, unit);
	}
	
	public static void shutdown() {
		synchronized (mutex) {
			if (executor != null) {
				executor.shutdown();
			}
		}
	}
	
}
