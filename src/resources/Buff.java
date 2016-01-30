package resources;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.encodables.Encodable;

public class Buff implements Encodable, Serializable {

	private static final long serialVersionUID = 1;
	
	private int duration;
	private long bufferId;
	private int endTime;
	private long stackCount;
	private float skillMod1Value;
	
	public Buff(long bufferId, int playTime, int duration, float skillMod1Value) {
		this.bufferId = bufferId;
		this.duration = duration;
		this.skillMod1Value = skillMod1Value;
		endTime = duration + playTime;
		stackCount = 1;
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer data = ByteBuffer.allocate(Integer.BYTES * 2 + Float.BYTES + Long.BYTES * 2 + Short.BYTES ).order(ByteOrder.LITTLE_ENDIAN);
		
		data.putInt(endTime);	// Buff duration + time played on character
		data.putFloat(skillMod1Value);	// The value for skillMod #1 on the buff. Displayed on the client as skillMod1Value * stackCount.
		data.putInt(duration);	// Icon shadow "clock" overlay.
		data.putLong(bufferId);	// Object ID of the buffer
		data.putLong(stackCount);	// Stack count
		data.putShort((short) 0);	// Unknown. Could also be two bytes.
		
		return data.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		endTime = data.getInt();
		skillMod1Value = data.getFloat();
		duration = data.getInt();
		bufferId = data.getLong();
		stackCount = data.getLong();
		data.getShort();	// unknown
	}

	public int getDuration() {
		return duration;
	}

	public long getStackCount() {
		return stackCount;
	}

	/**
	 * Adjusts the stack count. This method doesn't send any delta.
	 * @param adjustment
	 */
	public void adjustStackCount(int adjustment) {
		stackCount += adjustment;
	}
	
	public int getEndTime() {
		return endTime;
	}
	
}
