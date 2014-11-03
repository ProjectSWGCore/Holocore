package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;


public class CommandQueueEnqueue extends ObjectController {
	
	public static final int CRC = 0x0116;
	private int counter = 0;
	private int crc = 0;
	private long targetId = 0;
	private String arguments = "";
	
	public CommandQueueEnqueue() {
		
	}
	
	public CommandQueueEnqueue(ByteBuffer data) {
		decodeAsObjectController(data);
	}
	
	public void decodeAsObjectController(ByteBuffer data) {
		counter = getInt(data);
		crc = getInt(data);
		targetId = getLong(data);
		arguments = getUnicode(data);
	}
	
	public ByteBuffer encodeAsObjectController() {
		int length = 20 + arguments.length() * 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addInt(data, counter);
		addInt(data, crc);
		addLong(data, targetId);
		addUnicode(data, arguments);
		return data;
	}
	
}
