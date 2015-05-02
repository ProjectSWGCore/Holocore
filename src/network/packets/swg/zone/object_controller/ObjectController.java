package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public abstract class ObjectController extends SWGPacket {
	
	public static final int CRC = 0x80CE5E46;
	protected static final int HEADER_LENGTH = 26;
	
	private final int controllerCrc;
	private int update = 0;
	private long objectId = 0;
	
	public ObjectController(int controllerCrc) {
		this(0, controllerCrc);
	}
	
	public ObjectController(long objectId, int controllerCrc) {
		this.objectId = objectId;
		this.controllerCrc = controllerCrc;
		this.update = 11;
	}
	
	protected final void decodeHeader(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		update = getInt(data);
		if (getInt(data) != controllerCrc)
			System.err.println("ObjectController[" + getClass().getSimpleName() + "] Attempting to process invalid controller");
		objectId = getLong(data);
		getInt(data);
		return;
	}
	
	protected final void encodeHeader(ByteBuffer data) {
		addShort(data, 2);
		addInt(  data, CRC);
		addInt  (data, update);
		addInt(  data, controllerCrc);
		addLong( data, objectId);
		addInt(  data, 0);
	}
	
	public abstract void decode(ByteBuffer data);
	public abstract ByteBuffer encode();
	
	public long getObjectId() { return objectId; }
	public int getUpdate() { return update; }
	public int getControllerCrc() { return controllerCrc; }
	
	public void setUpdate(int update) { this.update = update; }
	
	public static final ObjectController decodeController(ByteBuffer data) {
		if (data.array().length < 14)
			return null;
		int crc = data.getInt(10);
		switch (crc) {
			case 0x00000071: return new DataTransform(data);
			case 0x00000116: return new CommandQueueEnqueue(data);
			case 0x00000117: return new CommandQueueDequeue(data);
			case 0x0000012E: return new PlayerEmote(data);
			case 0x00000131: return new PostureUpdate(data);
			case 0x00000146: return new ObjectMenuRequest(data);
		}
		return null;
	}
	
}
