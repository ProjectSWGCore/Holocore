package resources.encodables;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import resources.network.BaselineBuilder.Encodable;

public class Stf implements Encodable {
	private static final long serialVersionUID = 1L;
	
	private String key = "";
	private String file = "";
	
	public Stf(String file, String key) {
		this.file = file;
		this.key = key;
	}
	
	public Stf(String stf) {
		if (!stf.contains(":")) {
			System.err.println("Stf: Invalid stf format! Expected a semi-colon for " + stf);
			return;
		}
		
		if (stf.startsWith("@")) stf = stf.replaceFirst("@", "");
		
		String[] split = stf.split(":");
		file = split[0];
		
		if (split.length == 2)
			key = split[1];
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer = ByteBuffer.allocate(8 + key.length() + file.length()).order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putShort((short) file.length());
		buffer.put(file.getBytes(Charset.forName("UTF-8")));
		buffer.putInt(0);
		buffer.putShort((short) key.length());
		buffer.put(key.getBytes(Charset.forName("UTF-8")));
		
		return buffer.array();
	}

	public String getKey() { return key; }
	public void setKey(String key) { this.key = key; }

	public String getFile() { return file; }
	public void setFile(String file) { this.file = file; }

	@Override
	public String toString() {
		return "@" + file + ":" + key;
	}
}
