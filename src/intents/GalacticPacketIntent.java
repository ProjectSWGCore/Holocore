package intents;

import network.packets.Packet;
import resources.network.ServerType;

public class GalacticPacketIntent extends GalacticIntent {
	
	public static final String TYPE = "GalacticPacketIntent";
	
	private Packet packet;
	private ServerType type;
	private long networkId;
	
	public GalacticPacketIntent(InboundPacketIntent i) {
		super(TYPE);
		setPacket(i.getPacket());
		setServerType(i.getServerType());
		setNetworkId(i.getNetworkId());
	}
	
	public GalacticPacketIntent(ServerType type, Packet p, long networkId) {
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
