package network.packets.soe;

import java.nio.ByteBuffer;
import network.packets.Packet;


public class ClientNetworkStatusUpdate extends Packet {
	
	private int clientTickCount;
	private int lastUpdate;
	private int avgUpdate;
	private int shortUpdate;
	private int longUpdate;
	private int lastServerUpdate;
	private long packetSent;
	private long packetRecv;
	
	public ClientNetworkStatusUpdate() {
		
	}
	
	public ClientNetworkStatusUpdate(ByteBuffer data) {
		decode(data);
	}
	
	public ClientNetworkStatusUpdate(int clientTickCount, int lastUpdate, int avgUpdate, int shortUpdate, int longUpdate, int lastServerUpdate, long packetsSent, long packetsRecv) {
		this.clientTickCount = clientTickCount;
		this.lastUpdate = lastUpdate;
		this.avgUpdate = avgUpdate;
		this.shortUpdate = shortUpdate;
		this.longUpdate = longUpdate;
		this.lastServerUpdate = lastServerUpdate;
		this.packetSent = packetsSent;
		this.packetRecv = packetsRecv;
	}
	
	public void decode(ByteBuffer data) {
		data.position(2);
		clientTickCount  = getNetShort(data);
		lastUpdate       = getNetInt(data);
		avgUpdate        = getNetInt(data);
		shortUpdate      = getNetInt(data);
		longUpdate       = getNetInt(data);
		lastServerUpdate = getNetInt(data);
		packetSent       = getNetLong(data);
		packetRecv       = getNetLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(40);
		addNetShort(data, 7);
		addNetShort(data, clientTickCount);
		addNetInt(  data, lastUpdate);
		addNetInt(  data, avgUpdate);
		addNetInt(  data, shortUpdate);
		addNetInt(  data, longUpdate);
		addNetInt(  data, lastServerUpdate);
		addNetLong( data, packetSent);
		addNetLong( data, packetRecv);
		return data;
	}
	
	public int getTick() { return clientTickCount; }
	public int getLastUpdate() { return lastUpdate; }
	public int getAverageUpdate() { return avgUpdate; }
	public int getShortestUpdate() { return shortUpdate; }
	public int getLongestUpdate() { return longUpdate; }
	public int getLastServerUpdate() { return lastServerUpdate; }
	public long getSent() { return packetSent; }
	public long getRecv() { return packetRecv; }
	
	public void setTick(int tick) { this.clientTickCount = tick; }
	public void setLastUpdate(int last) { this.lastUpdate = last; }
	public void setAverageUpdate(int avg) { this.avgUpdate = avg; }
	public void setShortestUpdate(int shortest) { this.shortUpdate = shortest; }
	public void setLongestUpdate(int longest) { this.longUpdate = longest; }
	public void setLastServerUpdate(int last) { this.lastServerUpdate = last; }
	public void setPacketsSent(long sent) { this.packetSent = sent; }
	public void setPacketsRecv(long recv) { this.packetRecv = recv; }
}
