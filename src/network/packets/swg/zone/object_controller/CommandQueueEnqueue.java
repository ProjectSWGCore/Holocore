package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;


public class CommandQueueEnqueue extends ObjectController {
	
	public static final int CRC = 0x0116;
	
	private int counter = 0;
	private int crc = 0;
	private long targetId = 0;
	private String arguments = "";
	
	public CommandQueueEnqueue(long objectId) {
		super(objectId, CRC);
	}
	
	public CommandQueueEnqueue(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		counter = getInt(data);
		crc = getInt(data);
		targetId = getLong(data);
		arguments = getUnicode(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 20 + arguments.length()*2);
		encodeHeader(data);
		addInt(data, counter);
		addInt(data, crc);
		addLong(data, targetId);
		addUnicode(data, arguments);
		return data;
	}
	
	public int getCommandCrc() { return crc; }
	public String getArguments() { return arguments; }
	public long getTargetId() { return targetId; }
	
	public void setCommandCrc(int crc) { this.crc = crc; }
	public void setArguments(String args) { this.arguments = args; }
	public void setTargetId(long targetId) { this.targetId = targetId; }
	
}
