package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;


public class CommandQueueDequeue extends ObjectController {
	
	public static final int CRC = 0x0117;
	
	private int counter;
	private float timer;
	private int error;
	private int action;
	
	public CommandQueueDequeue(long objectId) {
		super(objectId, CRC);
	}
	
	public CommandQueueDequeue(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		counter = getInt(data);
		timer = getFloat(data);
		error = getInt(data);
		action = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 16);
		encodeHeader(data);
		addInt(data, counter);
		addFloat(data, timer);
		addInt(data, error);
		addInt(data, action);
		return data;
	}
	
	public int getCounter() { return counter; }
	public float getTimer() { return timer; }
	public int getError() { return error; }
	public int getAction() { return action; }
	
	public void setCounter(int counter) { this.counter = counter; }
	public void setTimer(float timer) { this.timer = timer; }
	public void setError(int error) { this.error = error; }
	public void setAction(int action) { this.action = action; }
	
}
