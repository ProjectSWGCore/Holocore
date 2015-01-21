package intents;

import network.packets.Packet;
import resources.Terrain;
import resources.control.Intent;

public class NotifyPlayersPacketIntent extends Intent {

	public static final String TYPE = "GalaxyWidePacketIntent";
	
	private Packet packet;
	private Terrain terrain;
	
	public NotifyPlayersPacketIntent(Packet packet, Terrain terrain) {
		super(TYPE);
		this.packet = packet;
		this.terrain = terrain;
	}
	public NotifyPlayersPacketIntent(Packet p) {
		this(p, null);
	}

	public Packet getPacket() { return packet; }
	public Terrain getTerrain() { return terrain; }
}
