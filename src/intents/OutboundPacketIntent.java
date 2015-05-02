package intents;

import network.packets.Packet;
import resources.control.Intent;

public class OutboundPacketIntent extends Intent {
	
	public static final String TYPE = "OutboundPacketIntent";
	
	private Packet packet;
	private long networkId;
	
	public OutboundPacketIntent(Packet p, long networkId) {
		super(TYPE);
		setPacket(p);
		setNetworkId(networkId);
	}
	
	public void setPacket(Packet p) {
		this.packet = p;
	}
	
	public void setNetworkId(long networkId) {
		this.networkId = networkId;
	}
	
	public Packet getPacket() {
		return packet;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
}
