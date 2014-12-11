package resources.encodables.lang;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import resources.network.BaselineBuilder.Encodable;

public class AString implements Encodable {
	private static final long serialVersionUID = 1L;
	
	private String string;
	
	public AString(String str) {
		this.string = str;
	}
	
	public String get() { return string; }
	public void set(String str) { this.string = str; }
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer = ByteBuffer.allocate(2 + string.length()).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) string.length());
		buffer.put(string.getBytes(Charset.forName("UTF-8")));
		return buffer.array();
	}
	
	public static AString value(String str) {
		return new AString(str);
	}
}
