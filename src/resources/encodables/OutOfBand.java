package resources.encodables;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.network.BaselineBuilder.Encodable;

public class OutOfBand implements Encodable {
	// Multiple ProsePackages, need to find an example of it in a packet. Also note that OutOfBand's did not have just ProsePackages.
	
	private static final long serialVersionUID = 1L;

	private ProsePackage prose;
	
	public OutOfBand(ProsePackage prose) {
		this.prose = prose;
	}
	
	public static OutOfBand ProsePackage(Object ... objects) {
		return new OutOfBand(new ProsePackage(objects));
	}
	
	public static OutOfBand ProsePackage(Object stf, String key, Object prose) {
		return new OutOfBand(new ProsePackage(stf, key, prose));
	}
	
	@Override
	public byte[] encode() {
		byte[] encodedProse = prose.encode();
		
		ByteBuffer bb = ByteBuffer.allocate(4 + encodedProse.length).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(encodedProse.length / 2);
		bb.put(encodedProse);
		
		return bb.array();
	}

}
