package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MissionListRequest extends ObjectController{
	
	public static final int CRC = 0x00F5;

	private long terminalId;
	private int tickCount;	
	
	public MissionListRequest(ByteBuffer data) {
		super(CRC);
	}
	
	public long getTerminalId() {
		return terminalId;
	}

	public void setTerminalId(long terminalId) {
		this.terminalId = terminalId;
	}

	public int getTickCount() {
		return tickCount;
	}

	public void setTickCount(int tickCount) {
		this.tickCount = tickCount;
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		data.get();
		setTickCount(data.get());
		setTerminalId(data.getLong());		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 10);
		encodeHeader(data);
		addByte(data, 0);
		addByte(data, getTickCount());
		addLong(data, getTerminalId());			
		return data;
	}
}