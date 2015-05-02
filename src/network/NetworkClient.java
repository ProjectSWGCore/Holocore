package network;

import intents.InboundPacketIntent;

import java.net.InetAddress;
import java.util.List;

import resources.control.Intent;
import resources.network.ServerType;
import network.packets.Packet;

public class NetworkClient {
	
	private final Object prevPacketIntentMutex = new Object();
	private final long networkId;
	private final ServerType serverType;
	private final NetworkProtocol protocol;
	private InetAddress address;
	private Intent prevPacketIntent;
	private int port;
	private int connId;
	
	public NetworkClient(ServerType type, InetAddress addr, int port, long networkId) {
		this.serverType = type;
		this.networkId = networkId;
		protocol = new NetworkProtocol(type, addr, port);
		prevPacketIntent = null;
		connId = 0;
		updateNetworkInfo(addr, port);
	}
	
	public void updateNetworkInfo(InetAddress addr, int port) {
		protocol.updateNetworkInfo(addr, port);
		this.address = addr;
		this.port = port;
	}
	
	public void resetNetwork() {
		protocol.resetNetwork();
		connId = 0;
	}
	
	public void setCrc(int crc) {
		protocol.setCrc(crc);
	}
	
	public void setConnectionId(int id) {
		connId = id;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getCrc() {
		return protocol.getCrc();
	}
	
	public int getConnectionId() {
		return connId;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public void sendPacket(Packet p) {
		protocol.sendPacket(p);
	}
	
	public boolean processPacket(ServerType type, byte [] data) {
		if (type != serverType || type == ServerType.UNKNOWN)
			return false;
		if (type == ServerType.PING)
			return true;
		List <Packet> packets = protocol.process(data);
		for (Packet p : packets) {
			p.setAddress(address);
			p.setPort(port);
			synchronized (prevPacketIntentMutex) {
				InboundPacketIntent i = new InboundPacketIntent(type, p, networkId);
				if (prevPacketIntent == null)
					i.broadcast();
				else
					i.broadcastAfterIntent(prevPacketIntent);
				prevPacketIntent = i;
			}
		}
		return packets.size() > 0;
	}
	
	public String toString() {
		return "NetworkClient[ConnId=" + connId + " " + address + ":" + port + "]";
	}
	
}
