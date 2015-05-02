package network.packets.soe;

import java.nio.ByteBuffer;

import network.packets.Packet;


public class ServerNetworkStatusUpdate extends Packet {
	
	private short clientTickCount = 0;
	private int serverSyncStampLong = 0;
	private long clientPacketsSent = 0;
	private long clientPacketsRecv = 0;
	private long serverPacketsSent = 0;
	private long serverPacketsRecv = 0;
	
	public ServerNetworkStatusUpdate() {
		
	}
	
	public ServerNetworkStatusUpdate(ByteBuffer data) {
		decode(data);
	}
	
	public ServerNetworkStatusUpdate(int clientTickCount, long clientSent, long clientRecv, long serverSent, long serverRecv) {
		this.clientTickCount = (short) clientTickCount;
		this.serverSyncStampLong = 0;
		this.clientPacketsSent = clientSent;
		this.clientPacketsRecv = clientRecv;
		this.serverPacketsSent = serverSent;
		this.serverPacketsRecv = serverRecv;
	}
	
	public void decode(ByteBuffer data) {
		data.position(2);
		clientTickCount     = getNetShort(data);
		serverSyncStampLong = getNetInt(data);
		clientPacketsSent   = getNetLong(data);
		clientPacketsRecv   = getNetLong(data);
		serverPacketsSent   = getNetLong(data);
		serverPacketsRecv   = getNetLong(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(40);
		addNetShort(data, 8);
		addNetShort(data, clientTickCount);
		addNetInt(  data, serverSyncStampLong);
		addNetLong( data, clientPacketsSent);
		addNetLong( data, clientPacketsRecv);
		addNetLong( data, serverPacketsSent);
		addNetLong( data, serverPacketsRecv);
		return data;
	}
	
	public short getClientTickCount()   { return clientTickCount; }
	public int getServerSyncStampLong() { return serverSyncStampLong; }
	public long getClientPacketsSent()  { return clientPacketsSent; }
	public long getClientPacketsRecv()  { return clientPacketsRecv; }
	public long getServerPacketsSent()  { return serverPacketsSent; }
	public long getServerPacketsRecv()  { return serverPacketsRecv; }
	
	public void setClientTickCount(short tick)   { this.clientTickCount = tick; }
	public void setServerSyncStampLong(int sync) { this.serverSyncStampLong = sync; }
	public void setClientPacketsSent(int sent)   { this.clientPacketsSent = sent; }
	public void setClientPacketsRecv(int recv)   { this.clientPacketsRecv = recv; }
	public void setServerPacketsSent(int sent)   { this.serverPacketsSent = sent; }
	public void setServerPacketsRecv(int recv)   { this.serverPacketsRecv = recv; }
}
