package network.packets.swg.zone.deltas;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;
import network.packets.swg.zone.baselines.Baseline.BaselineType;

public class DeltasMessage extends SWGPacket {
	
	public static final int CRC = 0x12862153;
	
	private long objId;
	private BaselineType type;
	private int num;
	private byte [] deltaData;
	private int update;
	
	public DeltasMessage() { }
	
	public DeltasMessage(long objId, BaselineType type, int typeNumber, byte [] data) {
		this.objId = objId;
		this.type = type;
		this.num = typeNumber;
		this.deltaData = data;
	}
	
	public DeltasMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objId = getLong(data);
		byte [] str = new byte[4]; data.get(str);
		String strType = new StringBuffer(new String(str, ascii)).reverse().toString();
		for (BaselineType baseType : BaselineType.values())
			if (baseType.toString().equals(strType))
				type = baseType;
		num = getByte(data);
		int length = getInt(data);
		this.deltaData = getArray(data, length);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(27 + deltaData.length);
		addShort(data, 5);
		addInt(  data, CRC);
		addLong(data, objId);
		data.put(new StringBuffer(type.toString()).reverse().toString().getBytes(ascii));
		addByte(data, num);
		addInt(data, deltaData.length + 4);
		addShort(data, 1); // TODO: How many updates there is (1 always for now until a queue system is built
		addShort(data, update); // TODO: This should be part of the data, for now we'll keep it here until the queue system is in
		data.put(deltaData);
		return data;
	}
	
	public long getObjectId() { return objId; }
	public BaselineType getType() { return type; }
	public int getNum() { return num; }
	public byte [] getDeltaData() { return deltaData; }
	
	public void setType(BaselineType type) { this.type = type; }
	public void setNum(int num) { this.num = num; }
	public void setId(long id) { this.objId = id; }
	public void setData(byte[] data) { this.deltaData = data; }
	public void setUpdate(int update) { this.update = update; }
}
