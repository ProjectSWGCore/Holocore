package network.packets.swg.zone.object_controller;

import resources.Location;

import java.nio.ByteBuffer;

/**
 * @author Waverunner
 */
public class DataTransformWithParent extends ObjectController {
	public static final int CRC = 0x00F1;

	private int timestamp;
	private int counter;
	private long cellId;
	private Location l;
	private float speed;
	private float lookAtYaw;
	private boolean useLookAtYaw;

	public DataTransformWithParent(long objectId) {
		super(objectId, CRC);
	}

	public DataTransformWithParent(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		timestamp = getInt(data); // Timestamp
		counter = getInt(data);
		cellId = getLong(data);
		l = getLocation(data);
		speed = getFloat(data);
		lookAtYaw = getFloat(data);
		useLookAtYaw = getBoolean(data);
	}

	@Override
	public ByteBuffer encode() {
		return null;
	}

	public int getCounter() {
		return counter;
	}

	public long getCellId() {
		return cellId;
	}

	public Location getLocation() {
		return l;
	}

	public float getSpeed() {
		return speed;
	}

	public float getLookAtYaw() {
		return lookAtYaw;
	}

	public boolean isUseLookAtYaw() {
		return useLookAtYaw;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public byte getMovementAngle() {
		byte movementAngle = (byte) 0.0f;
		double wOrient = l.getOrientationW();
		double yOrient = l.getOrientationY();
		double sq = Math.sqrt(1 - (wOrient*wOrient));

		if (sq != 0) {
			if (l.getOrientationW() > 0 && l.getOrientationY() < 0) {
				wOrient *= -1;
				yOrient *= -1;
			}
			movementAngle = (byte) ((yOrient / sq) * (2 * Math.acos(wOrient) / 0.06283f));
		}

		return movementAngle;
	}
}
