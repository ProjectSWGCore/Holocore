package network.packets.soe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import network.packets.Packet;


public class SessionRequest extends Packet {
	
	private int crcLength;
	private int connectionID;
	private int udpSize;
	
	public SessionRequest() {
		
	}
	
	public SessionRequest(ByteBuffer data) {
		decode(data);
	}
	
	public SessionRequest(int crcLength, int connectionID, int udpSize) {
		this.crcLength    = crcLength;
		this.connectionID = connectionID;
		this.udpSize      = udpSize;
	}
	
	public void decode(ByteBuffer packet) {
		super.decode(packet);
		packet.position(2);
		crcLength    = getNetInt(packet);
		connectionID = getNetInt(packet);
		udpSize      = getNetInt(packet);
	}
	
	public ByteBuffer encode() {
		ByteBuffer bb = ByteBuffer.allocate(14).order(ByteOrder.BIG_ENDIAN);
		addNetShort(bb, 1);
		addNetInt(bb, crcLength);
		addNetInt(bb, connectionID);
		addNetInt(bb, udpSize);
		return bb;
	}
	
	public int getCrcLength()    { return crcLength; }
	public int getConnectionID() { return connectionID; }
	public int getUdpSize()      { return udpSize; }
}
