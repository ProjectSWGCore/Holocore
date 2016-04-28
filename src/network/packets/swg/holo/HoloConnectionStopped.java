package network.packets.swg.holo;

import java.nio.ByteBuffer;

public class HoloConnectionStopped extends HoloPacket {
	
	public static final int CRC = resources.common.CRC.getCrc("HoloConnectionStopped");
	
	private ConnectionStoppedReason reason;
	
	public HoloConnectionStopped() {
		this(ConnectionStoppedReason.UNKNOWN);
	}
	
	public HoloConnectionStopped(ConnectionStoppedReason reason) {
		this.reason = reason;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		try {
			reason = ConnectionStoppedReason.valueOf(getAscii(data));
		} catch (IllegalArgumentException e) {
			reason = ConnectionStoppedReason.UNKNOWN;
		}
	}
	
	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(8+reason.name().length());
		addShort(data, 1);
		addInt(data, CRC);
		addAscii(data, reason.name());
		return data;
	}
	
	public void setReason(ConnectionStoppedReason reason) {
		this.reason = reason;
	}
	
	public ConnectionStoppedReason getReason() {
		return reason;
	}
	
	public static enum ConnectionStoppedReason {
		APPLICATION,
		INVALID_PROTOCOL,
		OTHER_SIDE_TERMINATED,
		NETWORK,
		SERVER_ERROR,
		UNKNOWN
	}
	
}
