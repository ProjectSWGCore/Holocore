package resources.objects.creature;

import java.io.Serializable;
import java.nio.ByteBuffer;
import resources.encodables.Encodable;
import resources.network.NetBuffer;

public class Buff implements Encodable, Serializable {
	
	private static final long serialVersionUID = 1;
	
	private int endTime;
	private float value;
	private int duration;
	private long bufferId;
	private int stackCount;
	
	public Buff() {
		this(0, 0, 0, 0, 0);
	}
	
	public Buff(int endTime, float value, int duration, long buffer, int stackCount) {
		this.endTime = endTime;
		this.value = value;
		this.duration = duration;
		this.bufferId = buffer;
		this.stackCount = stackCount;
	}
	
	@Override
	public void decode(ByteBuffer data) {
		endTime = data.getInt();
		value = data.getFloat();
		duration = data.getInt();
		bufferId = data.getLong();
		stackCount = data.getInt();
	}
	
	@Override
	public byte[] encode() {
		NetBuffer data = NetBuffer.allocate(24);
		data.addInt(endTime);
		data.addFloat(value);
		data.addInt(duration);
		data.addLong(bufferId);
		data.addInt(stackCount);
		return data.array();
	}
	
	public int getEndTime() {
		return endTime;
	}
	
	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}
	
	public float getValue() {
		return value;
	}
	
	public void setValue(float value) {
		this.value = value;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public long getBuffer() {
		return bufferId;
	}
	
	public void setBuffer(long buffer) {
		this.bufferId = buffer;
	}
	
	public int getStackCount() {
		return stackCount;
	}
	
	public void setStackCount(int stackCount) {
		this.stackCount = stackCount;
	}
	
	public void adjustStackCount(int adjust) {
		this.stackCount += adjust;
	}
	
	@Override
	public String toString() {
		return String.format("Buff[End=%d Value=%f Duration=%d Buffer=%d StackCount=%d]", endTime, value, duration, bufferId, stackCount);
	}
	
}
