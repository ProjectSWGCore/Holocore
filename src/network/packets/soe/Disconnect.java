package network.packets.soe;

import java.nio.ByteBuffer;

import network.packets.Packet;


public class Disconnect extends Packet {
	
	private int connectionId;
	private DisconnectReason reason;
	
	public Disconnect() {
		connectionId = 0;
		reason = DisconnectReason.NONE;
	}
	
	public Disconnect(int connectionId, DisconnectReason reason) {
		this.connectionId = connectionId;
		this.reason = reason;
	}
	
	public Disconnect(ByteBuffer data){
		this.decode(data);
	}
	
	public void decode(ByteBuffer data) {
		data.position(2);
		connectionId = getNetInt(data);
		reason       = getReason(getNetShort(data));
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8);
		addNetShort(data, 5);
		addNetInt(data, connectionId);
		addNetShort(data, reason.getReason());
		return data;
	}
	
	public int getConnectionID() { return connectionId; }
	public DisconnectReason getReason() { return reason; }
	
	private DisconnectReason getReason(int reason) {
		for (DisconnectReason dr : DisconnectReason.values())
			if (dr.getReason() == reason)
				return dr;
		return DisconnectReason.NONE;
	}
	
	public enum DisconnectReason {
		NONE					(0x00),
		ICMP_ERROR				(0x01),
		TIMEOUT					(0x02),
		OTHER_SIDE_TERMINATED	(0x03),
		MANAGER_DELETED			(0x04),
		CONNECT_FAIL			(0x05),
		APPLICATION				(0x06),
		UNREACHABLE_CONNECTION	(0x07),
		UNACKNOWLEDGED_TIMEOUT	(0x08),
		NEW_CONNECTION_ATTEMPT	(0x09),
		CONNECTION_REFUSED		(0x0A),
		MUTUAL_CONNETION_ERROR	(0x0B),
		CONNETING_TO_SELF		(0x0C),
		RELIABLE_OVERFLOW		(0x0D),
		COUNT					(0x0E);
		
		private short reason;
		
		DisconnectReason(int reason) {
			this.reason = (short) reason;
		}
		
		public short getReason() {
			return reason;
		}
	}
}
