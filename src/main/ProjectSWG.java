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
package main;

import intents.server.ServerStatusIntent;
import resources.Galaxy.GalaxyStatus;
import resources.control.IntentManager;
import resources.control.ServerStatus;
import services.CoreManager;

public class ProjectSWG {
	
	private static ProjectSWG server;
	private final Thread mainThread;
	private CoreManager manager;
	private boolean shutdownRequested;
	private ServerStatus status;
	
	public static final void main(String [] args) {
		server = new ProjectSWG();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				server.forceShutdown();
			}
		});
		try {
			server.run();
		} catch (CoreException e) {
			System.err.println("ProjectSWG: Shutting down. Reason: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("ProjectSWG: Shutting down - unknown error.");
		}
		server.stop();
		server.terminate();
		System.out.println("ProjectSWG: Server shut down.");
	}
	
	/**
	 * Returns the time in milliseconds since the server started initialization
	 * @return the core time represented as a double
	 */
	public static final long getCoreTime() {
		return (long) server.manager.getCoreTime();
	}
	
	/**
	 * Returns the server's galactic time. This is the official time sent to
	 * the client and should be used for any official client-time purposes.
	 * @return the server's galactic time
	 */
	public static final long getGalacticTime() {
		return (long) (System.currentTimeMillis()/1E3 - 1309996800L); // Date is 07/07/2011 GMT
	}
	
	private ProjectSWG() {
		mainThread = Thread.currentThread();
		shutdownRequested = false;
	}
	
	private void run() {
		long start = System.nanoTime();
		manager = new CoreManager();
		long end = System.nanoTime();
		System.out.println("ProjectSWG: Created new manager in " + (end-start)/1E6 + "ms");
		while (!shutdownRequested && !manager.isShutdownRequested()) {
			initialize();
			start();
			loop();
			stop();
			terminate();
			if (!shutdownRequested && !manager.isShutdownRequested()) {
				start = System.nanoTime();
				manager = new CoreManager();
				end = System.nanoTime();
				System.out.println("ProjectSWG: Created new manager in " + (end-start)/1E6 + "ms");
			}
		}
	}
	
	private void setStatus(ServerStatus status) {
		this.status = status;
		new ServerStatusIntent(status).broadcast();
	}
	
	private void forceShutdown() {
		shutdownRequested = true;
		mainThread.interrupt();
		try { mainThread.join(); } catch (InterruptedException e) { }
	}
	
	private void initialize() {
		setStatus(ServerStatus.INITIALIZING);
		System.out.println("ProjectSWG: Initializing...");
		if (!manager.initialize())
			throw new CoreException("Failed to initialize.");
		System.out.println("ProjectSWG: Initialized. Time: " + manager.getCoreTime() + "ms");
	}
	
	private void start() {
		System.out.println("ProjectSWG: Starting...");
		if (!manager.start())
			throw new CoreException("Failed to start.");
		System.out.println("ProjectSWG: Started. Time: " + manager.getCoreTime() + "ms");
	}
	
	private void loop() {
		setStatus((manager.getGalaxyStatus() == GalaxyStatus.UP) ? ServerStatus.OPEN : ServerStatus.LOCKED);
		while (!shutdownRequested && !manager.isShutdownRequested() && manager.isOperational()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				throw new CoreException("Main Thread Interrupted.");
			}
		}
	}
	
	private void stop() {
		if (manager == null || status == ServerStatus.OFFLINE)
			return;
		System.out.println("ProjectSWG: Stopping...");
		setStatus(ServerStatus.STOPPING);
		if (!manager.stop())
			System.err.println("Failed to stop.");
		long intentWait = System.nanoTime();
		while (IntentManager.getIntentsQueued() > 0 && System.nanoTime()-intentWait < 3E9) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				System.err.println("ProjectSWG: Failed to stop! Interrupted with " + IntentManager.getIntentsQueued() + " intents remaining");
				break;
			}
		}
		System.out.println("ProjectSWG: Stopped. Time: " + manager.getCoreTime() + "ms");
	}
	
	private void terminate() {
		if (manager == null || status == ServerStatus.OFFLINE)
			return;
		System.out.println("ProjectSWG: Terminating...");
		setStatus(ServerStatus.TERMINATING);
		if (!manager.terminate())
			throw new CoreException("Failed to terminate.");
		setStatus(ServerStatus.OFFLINE);
		System.out.println("ProjectSWG: Terminated. Time: " + manager.getCoreTime() + "ms");
	}
	
	public static class CoreException extends RuntimeException {
		
		private static final long serialVersionUID = 455306876887818064L;
		
		public CoreException(String reason) {
			super(reason);
		}
		
	}
	
}