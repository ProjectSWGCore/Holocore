package services;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import network.packets.Packet;
import network.packets.soe.DataChannelA;
import network.packets.soe.MultiPacket;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.baselines.Baseline;
import intents.InboundPacketIntent;
import intents.OutboundPacketIntent;
import intents.ServerManagementIntent;
import resources.Galaxy;
import resources.Galaxy.GalaxyStatus;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.services.Config;
import services.galaxy.GalacticManager;

public class CoreManager extends Manager {
	
	private static final int galaxyId = 1;
	private static final boolean debugOutput = false;
	private static PrintStream packetOutput;
	
	static {
		try {
			packetOutput = new PrintStream(new FileOutputStream("packets.txt", false));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			packetOutput = System.out;
		}
	}
	
	private EngineManager engineManager;
	private GalacticManager galacticManager;
	private Galaxy galaxy;
	private long startTime;
	private boolean shutdownRequested;
	
	public CoreManager() {
		shutdownRequested = false;
		galaxy = getGalaxy();
		if (galaxy != null) {
			engineManager = new EngineManager(galaxy);
			galacticManager = new GalacticManager(galaxy);
			
			addChildService(engineManager);
			addChildService(galacticManager);
		}
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
		registerForIntent(InboundPacketIntent.TYPE);
		registerForIntent(OutboundPacketIntent.TYPE);
		registerForIntent(ServerManagementIntent.TYPE);
		return galaxy != null && super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (debugOutput) {
			if (i instanceof InboundPacketIntent) {
				InboundPacketIntent in = (InboundPacketIntent) i;
				packetOutput.println("IN  " + in.getNetworkId() + ":" + in.getServerType());
				outputPacket(1, in.getPacket());
			} else if (i instanceof OutboundPacketIntent) {
				OutboundPacketIntent out = (OutboundPacketIntent) i;
				packetOutput.println("OUT " + out.getNetworkId());
				outputPacket(1, out.getPacket());
			}
		}
		if (i instanceof ServerManagementIntent)
			handleServerManagementIntent((ServerManagementIntent) i);
	}
	
	private void handleServerManagementIntent(ServerManagementIntent i) {
		switch(i.getEvent()) {
		case SHUTDOWN: initiateShutdownSequence();  break;
		default: break;
		}
		
	}

	private void initiateShutdownSequence() {
		System.out.println("Beginning server shutdown sequence...");
		shutdownRequested = true;
	}
	
	public GalaxyStatus getGalaxyStatus() {
		return galaxy.getStatus();
	}
	
	private void outputPacket(int indent, Packet packet) {
		if (packet instanceof DataChannelA) {
			for (SWGPacket p : ((DataChannelA) packet).getPackets()) {
				for (int i = 0; i < indent; i++)
					packetOutput.print("    ");
				outputSWG(p);
			}
		} else if (packet instanceof MultiPacket) {
			for (Packet p : ((MultiPacket) packet).getPackets()) {
				for (int i = 0; i < indent; i++)
					packetOutput.print("    ");
				if (p instanceof SWGPacket)
					outputSWG((SWGPacket) p);
				if (p instanceof DataChannelA)
					outputPacket(indent+1, p);
			}
		} else if (packet instanceof SWGPacket) {
			for (int i = 0; i < indent; i++)
				packetOutput.print("    ");
			outputSWG((SWGPacket) packet);
		} else {
			for (int i = 0; i < indent; i++)
				packetOutput.print("    ");
			packetOutput.println(packet.getClass().getSimpleName());
		}
	}
	
	private void outputSWG(SWGPacket p) {
		if (p instanceof Baseline)
			packetOutput.println("Baseline " + ((Baseline)p).getType() + " " + ((Baseline)p).getNum());
		else
			packetOutput.println(p.getClass().getSimpleName());
	}
	
	/**
	 * Returns the time in milliseconds since the server started initialization
	 * @return the core time represented as a double
	 */
	public double getCoreTime() {
		return (System.nanoTime()-startTime)/1E6;
	}
	
	private Galaxy getGalaxy() {
		PreparedStatement getGalaxy = getLocalDatabase().prepareStatement("SELECT * FROM galaxies WHERE id = ?");
		Config c = getConfig(ConfigFile.PRIMARY);
		try {
			getGalaxy.setInt(1, galaxyId);
			ResultSet set = getGalaxy.executeQuery();
			if (!set.next()) {
				System.err.println("CoreManager: No such galaxy exists with ID " + galaxyId + "!");
				return null;
			}
			Galaxy g = new Galaxy();
			g.setId(set.getInt("id"));
			g.setName(set.getString("name"));
			g.setAddress(set.getString("address"));
			g.setPopulation(set.getInt("population"));
			g.setTimeZone(set.getInt("timezone"));
			g.setZonePort(set.getInt("zone_port"));
			g.setPingPort(set.getInt("ping_port"));
			g.setStatus(set.getInt("status"));
			g.setMaxCharacters(c.getInt("GALAXY-MAX-CHARACTERS", 2));
			g.setOnlinePlayerLimit(c.getInt("GALAXY-MAX-ONLINE", 3000));
			g.setOnlineFreeTrialLimit(c.getInt("GALAXY-MAX-ONLINE", 3000));
			g.setRecommended(true);
			return g;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
