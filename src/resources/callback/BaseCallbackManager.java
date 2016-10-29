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
package resources.callback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import utilities.ThreadUtilities;

class BaseCallbackManager {
	
	private final Object executorMutex;
	private final String name;
	private final int threadCount;
	private final AtomicBoolean shutdown;
	private ExecutorService executor;
	
	public BaseCallbackManager(String name) {
		this(name, 1);
	}
	
	public BaseCallbackManager(String name, int threadCount) {
		this.executorMutex = new Object();
		this.name = name;
		this.threadCount = threadCount;
		this.shutdown = new AtomicBoolean(false);
		this.executor = null;
	}
	
	public void start() {
		synchronized (executorMutex) {
			if (executor != null && !shutdown.get())
				executor.shutdown();
			shutdown.set(false);
			if (threadCount <= 1)
				executor = Executors.newSingleThreadExecutor(ThreadUtilities.newThreadFactory(name+"-callback-manager"));
			else
				executor = Executors.newFixedThreadPool(threadCount, ThreadUtilities.newThreadFactory(name+"-callback-manager-%d"));
		}
	}
	
	public void stop() {
		synchronized (executorMutex) {
			executor.shutdown();
			shutdown.set(true);
		}
	}
	
	public boolean awaitTermination(long timeout, TimeUnit unit) {
		synchronized (executorMutex) {
			if (executor == null)
				return true;
			if (!shutdown.get())
				throw new IllegalStateException("Cannot wait for termination when never stopped!");
			try {
				return executor.awaitTermination(timeout, unit);
			} catch (InterruptedException e) {
				return false;
			}
		}
	}
	
	protected boolean call(Runnable r) {
		synchronized (executorMutex) {
			if (executor == null)
				throw new IllegalStateException("Manager has not been started!");
			if (shutdown.get())
				return false;
			try {
				executor.execute(r);
				return true;
			} catch (RejectedExecutionException e) {
				return false;
			}
		}
	}
	
}
