package network.packets.swg;

import java.nio.ByteBuffer;

import resources.Location;
import network.PacketType;
import network.packets.Packet;


public class SWGPacket extends Packet {
	
	private ByteBuffer data = null;
	private int opcode = 0;
	private PacketType type = PacketType.UNKNOWN;
	
	protected void addLocation(ByteBuffer data, Location l) {
		addFloat(data, (float) (Double.isNaN(l.getOrientationX()) ? 0 : l.getOrientationX()));
		addFloat(data, (float) (Double.isNaN(l.getOrientationY()) ? 0 : l.getOrientationY()));
		addFloat(data, (float) (Double.isNaN(l.getOrientationZ()) ? 0 : l.getOrientationZ()));
		addFloat(data, (float) (Double.isNaN(l.getOrientationW()) ? 1 : l.getOrientationW()));
		addFloat(data, (float) (Double.isNaN(l.getX()) ? 0 : l.getX()));
		addFloat(data, (float) (Double.isNaN(l.getY()) ? 0 : l.getY()));
		addFloat(data, (float) (Double.isNaN(l.getZ()) ? 0 : l.getZ()));
	}
	
	protected Location getLocation(ByteBuffer data) {
		Location l = new Location();
		l.setOrientationX(getFloat(data));
		l.setOrientationY(getFloat(data));
		l.setOrientationZ(getFloat(data));
		l.setOrientationW(getFloat(data));
		l.setX(getFloat(data));
		l.setY(getFloat(data));
		l.setZ(getFloat(data));
		return l;
	}
	
	public void setSWGOpcode(int opcode) {
		this.opcode = opcode;
		this.type = PacketType.fromCrc(opcode);
	}
	
	public int getSWGOpcode() {
		return opcode;
	}
	
	public PacketType getPacketType() {
		return type;
	}
	
	public boolean decode(ByteBuffer data, int crc) {
		this.data = data;
		super.decode(data);
		data.position(2);
		setSWGOpcode(getInt(data));
		if (getSWGOpcode() != crc)
			return false;
		return true;
	}
	
	public void decode(ByteBuffer data) {
		this.data = data;
		if (data.array().length < 6)
			return;
		data.position(2);
		setSWGOpcode(getInt(data));
		data.position(0);
	}
	
	public ByteBuffer encode() {
		return data;
	}
	
	public ByteBuffer getData() {
		return data;
	}
}
