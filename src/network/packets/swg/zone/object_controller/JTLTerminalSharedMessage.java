package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class JTLTerminalSharedMessage extends ObjectController {

	public static final int CRC = 0x041C;
	
	private int tickCount;
	private long terminalId;	
	
	public JTLTerminalSharedMessage(ByteBuffer data) {
		super(CRC);
	}

	public int getTickCount() {
		return tickCount;
	}

	public void setTickCount(int tickCount) {
		this.tickCount = tickCount;
	}

	public long getTerminalId() {
		return terminalId;
	}

	public void setTerminalId(long terminalId) {
		this.terminalId = terminalId;
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		setTickCount(data.getInt());
		setTerminalId(data.getLong());	
		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 12);
		encodeHeader(data);
		addInt(data, getTickCount());
		addLong(data, getTerminalId());			
		return data;
	}
}