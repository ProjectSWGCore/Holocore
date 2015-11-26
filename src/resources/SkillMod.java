package resources;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.encodables.Encodable;

public class SkillMod implements Encodable, Serializable {
	
	private static final long serialVersionUID = 1L;

	private int base, modifier;
	
	public SkillMod(int base, int modifier) {
		this.base = base;
		this.modifier = modifier;
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 2).order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putInt(base);
		buffer.putInt(modifier);
		
		return buffer.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		base = data.getInt();
		modifier = data.getInt();
	}
	
	public void adjustBase(int adjustment) {
		base += adjustment;
	}
	
	public void adjustModifier(int adjustment) {
		modifier += adjustment;
	}
	
	public int getValue() {
		return base + modifier;
	}

}
