package network.packets.swg.zone.deltas;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;
import network.packets.swg.zone.baselines.Baseline.BaselineType;

public class DeltasMessage extends SWGPacket {
	
	public static final int CRC = 0x12862153;
	
	private long objId;
	private BaselineType type;
	private int num;
	private byte [] data;
	
	public DeltasMessage() {
		this(0, 0, (byte)0, new byte[0]);
	}
	
	public DeltasMessage(long objId, int type, byte typeNumber, byte [] data) {
		
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
		this.data = getArray(data, length);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(23 + this.data.length);
		addShort(data, 2);
		addInt(  data, CRC);
		addLong(data, objId);
		data.put(new StringBuffer(type.toString()).reverse().toString().getBytes(ascii));
		addByte(data, num);
		addInt(data, this.data.length);
		data.put(data);
		return data;
	}
	
	public long getObjectId() { return objId; }
	public BaselineType getType() { return type; }
	public int getNum() { return num; }
	
	public void setType(BaselineType type) { this.type = type; }
	public void setNum(int num) { this.num = num; }
	public void setId(long id) { this.objId = id; }
	
}
