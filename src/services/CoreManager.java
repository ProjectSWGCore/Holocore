package services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import network.packets.soe.DataChannelA;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.baselines.Baseline;
import intents.InboundPacketIntent;
import intents.OutboundPacketIntent;
import resources.Galaxy;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.services.Config;
import services.galaxy.GalacticManager;

public class CoreManager extends Manager {
	
	private static final int galaxyId = 1;

	private EngineManager engineManager;
	private GalacticManager galacticManager;
	private Galaxy galaxy;
	private long startTime;
	
	public CoreManager() {
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
	
	@Override
	public boolean initialize() {
		startTime = System.nanoTime();
		registerForIntent(InboundPacketIntent.TYPE);
		registerForIntent(OutboundPacketIntent.TYPE);
		return galaxy != null && super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundPacketIntent) {
			/*InboundPacketIntent in = (InboundPacketIntent) i;
			System.out.println("IN  " + in.getNetworkId() + ":" + in.getServerType() + "\t" + in.getPacket().getClass().getSimpleName());
			if (in.getPacket() instanceof DataChannelA) {
				for (SWGPacket p : ((DataChannelA) in.getPacket()).getPackets()) {
					System.out.println("    " + p.getClass().getSimpleName());
				}
			}*/
		} else if (i instanceof OutboundPacketIntent) {
			/*OutboundPacketIntent out = (OutboundPacketIntent) i;
			System.out.println("OUT " + out.getNetworkId() + "     \t" + out.getPacket().getClass().getSimpleName());
			if (out.getPacket() instanceof DataChannelA) {
				for (SWGPacket p : ((DataChannelA) out.getPacket()).getPackets()) {
					if (p instanceof Baseline)
						System.out.println("    Baseline " + ((Baseline)p).getType() + " " + ((Baseline)p).getNum());
					else
						System.out.println("    " + p.getClass().getSimpleName());
				}
			}*/
		}
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
