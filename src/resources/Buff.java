package resources;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import resources.encodables.Encodable;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;

public class Buff implements Encodable, Serializable {

	private static final long serialVersionUID = 1;
	
	private int duration;
	private final CreatureObject buffer, receiver;
	private final long endTime;
	private long stackCount;
	private float skillMod1Value;
	
	public Buff(CreatureObject buffer, CreatureObject receiver, int duration, float skillMod1Value) {
		this.buffer = buffer;
		this.receiver = receiver;
		this.duration = duration;
		this.skillMod1Value = skillMod1Value;
		endTime = getEndTimeFromNow();
		stackCount = 1;
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer data = ByteBuffer.allocate(Integer.BYTES * 2 + Float.BYTES + Long.BYTES * 2 + Short.BYTES ).order(ByteOrder.LITTLE_ENDIAN);
		int sentDuration = duration;
		PlayerObject playerObject = receiver.getPlayerObject();
		
		if(playerObject != null) {
			// TODO how will this work with NPCs?
			sentDuration += playerObject.getPlayTime();
		}
		
		data.putInt(sentDuration);	// Buff duration + time played on character
		data.putFloat(skillMod1Value);	// The value for skillMod #1 on the buff. Displayed on the client as skillMod1Value * stackCount.
		data.putInt(duration);	// Icon shadow "clock" overlay.
		data.putLong(buffer.getObjectId());	// Object ID of the caster
		data.putLong(stackCount);	// Stack count
		data.putShort((short) 0);	// Unknown. Could also be two bytes.
		
		return data.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		// TODO Auto-generated method stub
		
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
	
	private long getEndTimeFromNow() {
		return System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(duration, TimeUnit.SECONDS);
	}

	public long getRemainingTime() {
		return endTime - System.currentTimeMillis();
	}
	
	public long getEndTime() {
		return endTime;
	}
	
}
