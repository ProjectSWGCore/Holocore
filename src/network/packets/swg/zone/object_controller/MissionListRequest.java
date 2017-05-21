package network.packets.swg.zone.object_controller;

import com.projectswg.common.network.NetBuffer;

public class MissionListRequest extends ObjectController{
	
	public static final int CRC = 0x00F5;

	private long terminalId;
	private byte tickCount;	
	
	public MissionListRequest(NetBuffer data) {
		super(CRC);
	}
	
	public long getTerminalId() {
		return terminalId;
	}

	public void setTerminalId(long terminalId) {
		this.terminalId = terminalId;
	}

	public byte getTickCount() {
		return tickCount;
	}

	public void setTickCount(byte tickCount) {
		this.tickCount = tickCount;
	}

	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		data.getByte();
		setTickCount(data.getByte());
		setTerminalId(data.getLong());		
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 10);
		encodeHeader(data);
		data.addByte(0);
		data.addByte(getTickCount());
		data.addLong(getTerminalId());			
		return data;
	}
}