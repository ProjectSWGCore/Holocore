package network.packets.swg.zone.baselines;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class Baseline extends SWGPacket {
	
	public static final int CRC = 0x68A75F0C;
	private BaselineType type;
	private int num;
	private short opCount;
	private long objId;
	private byte [] baseData;
	
	public Baseline() {
		
	}
	
	public Baseline(long objId, Baseline subData) {
		this.objId = objId;
		type = subData.getType();
		num = subData.getNum();
		baseData = subData.encodeBaseline().array();
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
		baseData = new byte[getInt(data)];
		data.get(baseData);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(25 + baseData.length);
		addShort(data, 5);
		addInt(  data, CRC);
		addLong( data, objId);
		data.put(new StringBuffer(type.toString()).reverse().toString().getBytes(ascii));
		addByte( data, num);
		addInt(  data, baseData.length + 2);
		addShort( data, (opCount == 0 ? 5 : opCount));
		data.put(baseData);
		// TODO: It seems that baselines are being called to encode 3 times, might be possible they're also being sent 3 times as well...
		return data;
	}
	
	public ByteBuffer encodeBaseline() { return ByteBuffer.allocate(0); }
	
	public long getObjectId() { return objId; }
	
	public void setType(BaselineType type) { this.type = type; }
	public void setNum(int num) { this.num = num; }
	public void setId(long id) { this.objId = id; }
	public void setBaselineData(byte [] data) { this.baseData = data; }
	public void setOperandCount(int count) { this.opCount = (short) count;}
	
	public BaselineType getType() { return type; }
	public int getNum() { return num; }
	public long getId() { return objId; }
	public byte [] getBaselineData() { return baseData; }
	
	public enum BaselineType {
		BMRK, BUIO, CREO, FCYT,
		GILD, GRUP, HINO, INSO,
		ITNO, MINO, MISO, MSCO,
		PLAY, RCNO, SCLT, STAO,
		SHIP, TANO, WAYP, WEAO
	}
}
