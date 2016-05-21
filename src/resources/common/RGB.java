package resources.common;

import java.awt.Color;
import java.nio.ByteBuffer;

import resources.encodables.Encodable;
import resources.network.NetBuffer;

public class RGB implements Encodable {
	
	private byte r;
	private byte g;
	private byte b;
	
	public RGB() {
		this(0, 0, 0);
	}
	
	public RGB(int r, int g, int b) {
		setR(r);
		setG(g);
		setB(b);
	}
	
	public RGB(Color c) {
		this(c.getRed(), c.getGreen(), c.getBlue());
	}
	
	@Override
	public byte[] encode() {
		NetBuffer buffer = NetBuffer.allocate(3);
		buffer.addByte(r);
		buffer.addByte(g);
		buffer.addByte(b);
		return buffer.array();
	}
	
	@Override
	public void decode(ByteBuffer data) {
		r = data.get();
		g = data.get();
		b = data.get();
	}
	
	public byte getR() {
		return r;
	}
	
	public void setR(int r) {
		this.r = (byte) r;
	}
	
	public byte getG() {
		return g;
	}
	
	public void setG(int g) {
		this.g = (byte) g;
	}
	
	public byte getB() {
		return b;
	}
	
	public void setB(int b) {
		this.b = (byte) b;
	}
	
}
