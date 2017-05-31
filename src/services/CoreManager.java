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

import java.io.File;
import java.time.OffsetTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.projectswg.common.control.Manager;
import com.projectswg.common.data.info.Config;
import com.projectswg.common.debug.Log;

import intents.network.InboundPacketIntent;
import intents.network.OutboundPacketIntent;
import intents.server.ServerManagementIntent;
import intents.server.ServerStatusIntent;
import network.packets.Packet;
import network.packets.swg.admin.AdminShutdownServer;
import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.deltas.DeltasMessage;
import network.packets.swg.zone.object_controller.ObjectController;
import resources.Galaxy;
import resources.Galaxy.GalaxyStatus;
import resources.config.ConfigFile;
import resources.control.ServerStatus;
import resources.server_info.BasicLogStream;
import resources.server_info.DataManager;
import services.galaxy.GalacticManager;
import utilities.ScheduledUtilities;
import utilities.ThreadUtilities;

public class CoreManager extends Manager {
	
	private static final Galaxy GALAXY = new Galaxy();
	private static final int GALAXY_ID = 1;
	
	private final ScheduledExecutorService shutdownService;
	private final EngineManager engineManager;
	private final GalacticManager galacticManager;
	private final BasicLogStream packetLogger;
	private final Config debugConfig;
	
	private long startTime;
	private boolean shutdownRequested;
	
	public CoreManager(int adminServerPort) {
		debugConfig = DataManager.getConfig(ConfigFile.DEBUG);
		Config primaryConfig = DataManager.getConfig(ConfigFile.PRIMARY);
		setupGalaxy(primaryConfig);
		if (adminServerPort <= 0)
			adminServerPort = -1;
		getGalaxy().setAdminServerPort(adminServerPort);
		packetLogger = new BasicLogStream(new File("log/packets.txt"));
		shutdownService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("core-shutdown-service"));
		shutdownRequested = false;
		engineManager = new EngineManager();
		galacticManager = new GalacticManager();
		
		addChildService(engineManager);
		addChildService(galacticManager);
		
		registerForIntent(InboundPacketIntent.class, ipi -> handleInboundPacketIntent(ipi));
		registerForIntent(OutboundPacketIntent.class, opi -> handleOutboundPacketIntent(opi));
		registerForIntent(ServerManagementIntent.class, smi -> handleServerManagementIntent(smi));
	}
	
	/**
	 * Determines whether or not the core is operational
	 * @return TRUE if the core is operational, FALSE otherwise
	 */
	@Override
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
		ScheduledUtilities.shutdown();
		return super.terminate();
	}
	
	private void handleServerManagementIntent(ServerManagementIntent smi) {
		switch(smi.getEvent()) {
			case SHUTDOWN: initiateShutdownSequence(smi.getTime(), smi.getTimeUnit());  break;
			default: break;
		}
	}
	
	private void handleInboundPacketIntent(InboundPacketIntent ipi) {
		if (ipi.getPacket() instanceof AdminShutdownServer)
			initiateShutdownSequence(((AdminShutdownServer) ipi.getPacket()).getShutdownTime(), TimeUnit.SECONDS);
		if (!isPacketDebug())
			return;
		printPacketStream(true, ipi.getNetworkId(), createExtendedPacketInformation(ipi.getPacket()));
	}
	
	private void handleOutboundPacketIntent(OutboundPacketIntent opi) {
		if (!isPacketDebug())
			return;
		printPacketStream(false, opi.getNetworkId(), createExtendedPacketInformation(opi.getPacket()));
	}
	
	private void printPacketStream(boolean in, long networkId, String str) {
		packetLogger.log("%s %d:\t%s%n", in?"IN ":"OUT", networkId, str);
	}
	
	private boolean isPacketDebug() {
		return debugConfig.getBoolean("PACKET-LOGGING", false);
	}
	
	private String createExtendedPacketInformation(Packet p) {
		if (p instanceof Baseline)
			return createBaselineInformation((Baseline) p);
		if (p instanceof DeltasMessage)
			return createDeltaInformation((DeltasMessage) p);
		if (p instanceof ObjectController)
			return createControllerInformation((ObjectController) p);
		return p.getClass().getSimpleName();
	}
	
	private String createBaselineInformation(Baseline b) {
		return "Baseline:"+b.getType()+b.getNum()+"  ID="+b.getObjectId();
	}
	
	private String createDeltaInformation(DeltasMessage d) {
		return "Delta:"+d.getType()+d.getNum()+"  Var="+d.getUpdate()+"  ID="+d.getObjectId();
	}
	
	private String createControllerInformation(ObjectController c) {
		return "ObjectController:0x"+Integer.toHexString(c.getControllerCrc())+"  ID="+c.getObjectId();
	}
	
	private void initiateShutdownSequence(long time, TimeUnit unit) {
		Log.i("Beginning server shutdown sequence...");
		
		shutdownService.schedule(
				new Runnable() {
					@Override
					public void run() {
						shutdownRequested = true;
					}
					// Ziggy: Give the broadcast method extra time to complete.
					// If we don't, the final broadcast won't be displayed.
				},
				TimeUnit.NANOSECONDS.convert(time, unit) + TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS), TimeUnit.NANOSECONDS);

		new ServerStatusIntent(ServerStatus.SHUTDOWN_REQUESTED, time, unit).broadcast();
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
		GALAXY.setZoneOffset(OffsetTime.now().getOffset());
		GALAXY.setZonePort(0);
		GALAXY.setPingPort(0);
		GALAXY.setStatus(GalaxyStatus.DOWN);
		GALAXY.setMaxCharacters(c.getInt("GALAXY-MAX-CHARACTERS", 2));
		GALAXY.setOnlinePlayerLimit(c.getInt("GALAXY-MAX-ONLINE", 3000));
		GALAXY.setOnlineFreeTrialLimit(c.getInt("GALAXY-MAX-ONLINE", 3000));
		GALAXY.setRecommended(true);
	}
	
}
