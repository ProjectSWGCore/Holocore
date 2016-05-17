package resources.collections;

import resources.encodables.Encodable;
import resources.objects.SWGObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

public class SWGBitSet extends BitSet implements Encodable {

	private static final long serialVersionUID = 1L;
		
	private int view;
	private int updateType;


	public SWGBitSet(int view, int updateType) {
		super(128); // Seems to be the default size for the bitmask sets in packets
		this.view = view;
		this.updateType = updateType;
	}

	@Override
	public byte[] encode() {
		BitSet b = ((BitSet) this);
		byte[] bytes = toByteArray();
		ByteBuffer buffer = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(bytes.length);
		buffer.putInt(b.length());
		buffer.put(bytes).array();
		return buffer.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		// TODO: Decode method for SWGBitSet
		throw new UnsupportedOperationException("Unable to decode bitset!");
	}
		
	public void sendDeltaMessage(SWGObject target) {
		if (target.getOwner() == null) {
			return;
		}

		target.sendDelta(view, updateType, encode());
	}

}	
	

