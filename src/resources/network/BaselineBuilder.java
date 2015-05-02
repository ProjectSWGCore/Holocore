package resources.network;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.LinkedList;

import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.objects.SWGObject;
import resources.player.Player;

public class BaselineBuilder {
	
	public static final Charset ASCII   = Charset.forName("UTF-8");
	public static final Charset UNICODE = Charset.forName("UTF-16LE");
	
	private SWGObject object;
	private BaselineType type;
	private int num;
	private int opCount = 0;
	private LinkedList <byte []> data;
	private int size;
	
	public BaselineBuilder(SWGObject object, BaselineType type, int num) {
		this.object = object;
		this.type = type;
		this.num = num;
		data = new LinkedList<byte []>();
		size = 0;
	}
	
	public void sendTo(Player target) {
		byte [] data = build();
		Baseline baseline = new Baseline();
		baseline.setId(object.getObjectId());
		baseline.setType(type);
		baseline.setNum(num);
		baseline.setOperandCount(opCount);
		baseline.setBaselineData(data);
		target.sendPacket(baseline);
	}
	
	public byte [] buildAsBaselinePacket() {
		byte [] data = build();
		Baseline baseline = new Baseline();
		baseline.setId(object.getObjectId());
		baseline.setType(type);
		baseline.setNum(num);
		baseline.setOperandCount(opCount);
		baseline.setBaselineData(data);
		
		return baseline.encode().array();
	}
	
	public byte [] build() {
		byte [] data = new byte[size];
		int offset = 0;
		for (byte [] d : this.data) {
			System.arraycopy(d, 0, data, offset, d.length);
			offset += d.length;
		}
		return data;
	}
	
	public void addObject(Encodable e) {
		byte [] d = e.encode();
		size += d.length;
		data.add(d);
	}
	
	public void addBoolean(boolean b) {
		addByte(b ? 1 : 0);
	}
	
	public void addAscii(String str) {
		ByteBuffer bb = ByteBuffer.allocate(2 + str.length()).order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort((short) str.length());
		bb.put(str.getBytes(ASCII));
		data.add(bb.array());
		size += bb.array().length;
	}
	
	public void addUnicode(String str) {
		ByteBuffer bb = ByteBuffer.allocate(4 + str.length()*2).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(str.length());
		bb.put(str.getBytes(UNICODE));
		data.add(bb.array());
		size += bb.array().length;
	}
	
	public void addByte(int b) {
		data.add(new byte[]{(byte) b});
		size += 1;
	}
	
	public void addShort(int s) {
		ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort((short) s);
		data.add(bb.array());
		size += bb.array().length;
	}
	
	public void addInt(int i) {
		ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(i);
		data.add(bb.array());
		size += bb.array().length;
	}
	
	public void addLong(long l) {
		ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		bb.putLong(l);
		data.add(bb.array());
		size += bb.array().length;
	}
	
	public void addFloat(float f) {
		ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		bb.putFloat(f);
		data.add(bb.array());
		size += bb.array().length;
	}
	
	public void addArray(byte [] array) {
		ByteBuffer bb = ByteBuffer.allocate(2 + array.length).order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort((short) array.length);
		bb.put(array);
		data.add(bb.array());
		size += bb.array().length;
	}
	
	public int incrementOperandCount(int operands) {
		return opCount+=operands;
	}
	
	public interface Encodable extends Serializable {
		public byte [] encode();
	}
	
}
