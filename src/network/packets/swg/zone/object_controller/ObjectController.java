package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ObjectController extends SWGPacket {
	
	public static final int CRC = 0x80CE5E46;
	
	private int header2 = 0;
	private int update = 0;
	private long objId = 0;
	private ObjectController obj;
	
	public ObjectController() {
		
	}
	
	public ObjectController(int header, long objId, ObjectController obj) {
		this.header2 = header;
		this.objId = objId;
		this.obj = obj;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		update = getInt(data);
		header2 = getInt(data);
		objId = getLong(data);
		getInt(data);
		byte [] rData = new byte[data.remaining()];
		data.get(rData);
		getDecodingObject(objId, header2, ByteBuffer.wrap(rData));
	}
	
	public ByteBuffer encode() {
		byte [] objData = obj.encodeAsObjectController().array();
		int length = 26 + objData.length;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt  (data, update);
		addInt(  data, header2);
		addLong( data, objId);
		addInt(  data, 0);
		data.put(objData);
		return data;
	}
	
	private void getDecodingObject(long id, int header, ByteBuffer data) {
		String hex = new String(Integer.toHexString(header)).toUpperCase();
		obj = null;
		switch (header) {
			case 0x00000071: obj = new DataTransform(data); break;
			case 0x00000116: obj = new CommandQueueEnqueue(data); break;
			case 0x00000117: obj = new CommandQueueDequeue(data); break;
			case 0x00000131: obj = new PostureUpdate(data); break;
//			default: System.out.println("Unprocessed Object Controller: " + header + "[0x" + hex + "]"); break;
		}
	}
	
	public ByteBuffer encodeAsObjectController() { return ByteBuffer.allocate(0); }
	public void decodeAsObjectController(ByteBuffer data) { }
	
	public long getObjectId() { return objId; }
}
