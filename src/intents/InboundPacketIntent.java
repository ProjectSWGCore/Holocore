package intents;

import network.packets.Packet;
import resources.control.Intent;
import resources.network.ServerType;

public class InboundPacketIntent extends Intent {
	
	public static final String TYPE = "InboundPacketIntent";
	
	private Packet packet;
	private ServerType type;
	private long networkId;
	
	public InboundPacketIntent(ServerType type, Packet p, long networkId) {
		super(TYPE);
		setPacket(p);
		setServerType(type);
		setNetworkId(networkId);
	}
	
	public void setPacket(Packet p) {
		this.packet = p;
	}
	
	public void setServerType(ServerType type) {
		this.type = type;
	}
	
	public void setNetworkId(long networkId) {
		this.networkId = networkId;
	}
	
	public Packet getPacket() {
		return packet;
	}
	
	public ServerType getServerType() {
		return type;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
}
