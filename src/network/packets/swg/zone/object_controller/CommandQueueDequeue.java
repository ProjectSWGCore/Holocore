package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;


public class CommandQueueDequeue extends ObjectController {
	
	public static final int CRC = 0x0117;
	private int counter;
	private float timer;
	private int error;
	private int action;
	
	public CommandQueueDequeue() {
		
	}
	
	public CommandQueueDequeue(ByteBuffer data) {
		decodeAsObjectController(data);
	}
	
	public void decodeAsObjectController(ByteBuffer data) {
		counter = getInt(data);
		timer = getFloat(data);
		error = getInt(data);
		action = getInt(data);
	}
	
	public ByteBuffer encodeAsObjectController() {
		ByteBuffer data = ByteBuffer.allocate(16);
		addInt(data, counter);
		addFloat(data, timer);
		addInt(data, error);
		addInt(data, action);
		return data;
	}
	
	public int getCounter() {
		return counter;
	}
	
	public float getTimer() {
		return timer;
	}
	
	public int getError() {
		return error;
	}
	
	public int getAction() {
		return action;
	}
	
}
