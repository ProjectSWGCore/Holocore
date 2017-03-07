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
package resources.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import resources.control.Assert;

public class PswgThreadPool {
	
	private final AtomicBoolean running;
	private final int nThreads;
	private final ThreadFactory threadFactory;
	private ExecutorService executor;
	private volatile boolean waitForTermination;
	
	public PswgThreadPool(int nThreads, String nameFormat) {
		this.running = new AtomicBoolean(false);
		this.nThreads = nThreads;
		this.threadFactory = new CustomThreadFactory(nameFormat);
		this.executor = null;
		this.waitForTermination = false;
	}
	
	public void setWaitForTermination(boolean wait) {
		this.waitForTermination = wait;
	}
	
	public void start() {
		Assert.test(!running.getAndSet(true));
		executor = Executors.newFixedThreadPool(nThreads, threadFactory);
	}
	
	public void stop() {
		Assert.test(running.getAndSet(false));
		if (waitForTermination)
			executor.shutdownNow();
		else
			executor.shutdown();
	}
	
	public boolean execute(Runnable runnable) {
		if (!running.get())
			return false;
		executor.execute(() -> {
			try {
				runnable.run();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		});
		return true;
	}
	
	public boolean awaitTermination(long time) {
		Assert.notNull(executor);
		try {
			return executor.awaitTermination(time, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	public boolean isRunning() {
		return running.get();
	}
	
	private static class CustomThreadFactory implements ThreadFactory {
		
		private final String pattern;
		private int counter;
		
		public CustomThreadFactory(String pattern) {
			this.pattern = pattern;
			this.counter = 0;
		}
		
		@Override
		public Thread newThread(Runnable r) {
			String name;
			if (pattern.contains("%d"))
				name = String.format(pattern, counter++);
			else
				name = pattern;
			return new Thread(r, name);
		}
		
	}
	
}
