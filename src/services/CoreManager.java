package services;

import network.packets.soe.DataChannelA;
import network.packets.swg.SWGPacket;
import intents.InboundPacketIntent;
import intents.OutboundPacketIntent;
import resources.control.Intent;
import resources.control.Manager;
import services.player.PlayerManager;

public class CoreManager extends Manager {
	
	private long startTime;
	private EngineManager engineManager;
	private PlayerManager playerManager;
	
	public CoreManager() {
		engineManager = new EngineManager();
		playerManager = new PlayerManager();
		
		addChildService(engineManager);
		addChildService(playerManager);
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
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundPacketIntent) {
			InboundPacketIntent in = (InboundPacketIntent) i;
			System.out.println("IN  " + in.getNetworkId() + ":" + in.getServerType() + "\t" + in.getPacket().getClass().getSimpleName());
			if (in.getPacket() instanceof DataChannelA) {
				for (SWGPacket p : ((DataChannelA) in.getPacket()).getPackets()) {
					System.out.println("    " + p.getClass().getSimpleName());
				}
			}
		} else if (i instanceof OutboundPacketIntent) {
			OutboundPacketIntent out = (OutboundPacketIntent) i;
			System.out.println("OUT " + out.getNetworkId() + "     \t" + out.getPacket().getClass().getSimpleName());
			if (out.getPacket() instanceof DataChannelA) {
				for (SWGPacket p : ((DataChannelA) out.getPacket()).getPackets()) {
					System.out.println("    " + p.getClass().getSimpleName());
				}
			}
		}
	}
	
	/**
	 * Returns the time in milliseconds since the server started initialization
	 * @return the core time represented as a double
	 */
	public double getCoreTime() {
		return (System.nanoTime()-startTime)/1E6;
	}
	
}
