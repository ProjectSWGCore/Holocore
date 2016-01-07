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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import intents.network.InboundPacketIntent;
import intents.network.OutboundPacketIntent;
import intents.server.ServerManagementIntent;
import intents.server.ServerStatusIntent;
import resources.Galaxy;
import resources.Galaxy.GalaxyStatus;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.control.ServerStatus;
import resources.server_info.Config;
import resources.server_info.Log;
import resources.server_info.Log.LogLevel;
import services.admin.OnlineInterfaceService;
import services.galaxy.GalacticManager;
import utilities.ThreadUtilities;

public class CoreManager extends Manager {
	
	private static final Galaxy GALAXY = new Galaxy();
	private static final int GALAXY_ID = 1;
	
	private final ScheduledExecutorService shutdownService;
	
	private OnlineInterfaceService onlineInterfaceService;
	private EngineManager engineManager;
	private GalacticManager galacticManager;
	private long startTime;
	private boolean shutdownRequested;
	
	public CoreManager() {
		Config c = getConfig(ConfigFile.PRIMARY);
		Log.setLogLevel(LogLevel.valueOf(c.getString("LOG-LEVEL", LogLevel.DEBUG.name())));
		setupGalaxy(c);
		shutdownService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("core-shutdown-service"));
		shutdownRequested = false;
		onlineInterfaceService = new OnlineInterfaceService();
		engineManager = new EngineManager();
		galacticManager = new GalacticManager();
		
		addChildService(onlineInterfaceService);
		addChildService(engineManager);
		addChildService(galacticManager);
		
		registerForIntent(InboundPacketIntent.TYPE);
		registerForIntent(OutboundPacketIntent.TYPE);
		registerForIntent(ServerManagementIntent.TYPE);
	}
	
	/**
	 * Determines whether or not the core is operational
	 * @return TRUE if the core is operational, FALSE otherwise
	 */
	public boolean isOperational() {
		return true;
	}
	
	public boolean isShutdownRequested() {
		return shutdownRequested;
	}
	
	@Override
	public boolean initialize() {
		startTime = System.nanoTime();
		GALAXY.setStatus(GalaxyStatus.LOADING);
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		GALAXY.setStatus(GalaxyStatus.UP);
		return super.start();
	}
	
	@Override
	public boolean stop() {
		GALAXY.setStatus(GalaxyStatus.LOCKED);
		return super.stop();
	}
	
	@Override
	public boolean terminate() {
		shutdownService.shutdownNow();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof ServerManagementIntent)
			handleServerManagementIntent((ServerManagementIntent) i);
	}
	
	private void handleServerManagementIntent(ServerManagementIntent i) {
		switch(i.getEvent()) {
			case SHUTDOWN: initiateShutdownSequence(i);  break;
			default: break;
		}
	}
	
	private void initiateShutdownSequence(ServerManagementIntent i) {
		System.out.println("Beginning server shutdown sequence...");
		long time = i.getTime();
		TimeUnit timeUnit = i.getTimeUnit();
		
		shutdownService.schedule(
				new Runnable() {
					@Override
					public void run() {
						shutdownRequested = true;
					}
					// Ziggy: Give the broadcast method extra time to complete.
					// If we don't, the final broadcast won't be displayed.
				},
				TimeUnit.NANOSECONDS.convert(time, timeUnit) + TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS), TimeUnit.NANOSECONDS);

		new ServerStatusIntent(ServerStatus.SHUTDOWN_REQUESTED, time, i.getTimeUnit()).broadcast();
	}
	
	public GalaxyStatus getGalaxyStatus() {
		return GALAXY.getStatus();
	}
	
	/**
	 * Returns the time in milliseconds since the server started initialization
	 * @return the core time represented as a double
	 */
	public double getCoreTime() {
		return (System.nanoTime()-startTime)/1E6;
	}
	
	public static Galaxy getGalaxy() {
		return GALAXY;
	}
	
	public static int getGalaxyId() {
		return GALAXY_ID;
	}
	
	private static void setupGalaxy(Config c) {
		GALAXY.setId(GALAXY_ID);
		GALAXY.setName(c.getString("GALAXY-NAME", "Holocore"));
		GALAXY.setAddress("");
		GALAXY.setPopulation(0);
		GALAXY.setTimeZone(c.getInt("GALAXY-TIMEZONE", -6));
		GALAXY.setZonePort(0);
		GALAXY.setPingPort(0);
		GALAXY.setStatus(GalaxyStatus.DOWN);
		GALAXY.setMaxCharacters(c.getInt("GALAXY-MAX-CHARACTERS", 2));
		GALAXY.setOnlinePlayerLimit(c.getInt("GALAXY-MAX-ONLINE", 3000));
		GALAXY.setOnlineFreeTrialLimit(c.getInt("GALAXY-MAX-ONLINE", 3000));
		GALAXY.setRecommended(true);
	}
	
}
