package network.packets.swg.zone.spatial;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import network.packets.swg.SWGPacket;

public class AttributeListMessage extends SWGPacket {
	
	public static final int CRC = 0xF3F12F2A;
	
	private long objectId;
	private Map <String, String> attributes;
	
	public AttributeListMessage() {
		this(0, new HashMap<String, String>());
	}
	
	public AttributeListMessage(long objectId, Map <String, String> attributes) {
		this.objectId = objectId;
		this.attributes = attributes;
	}
	
	public AttributeListMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objectId = getLong(data);
		getShort(data);
		int count = getInt(data);
		for (int i = 0; i < count; i++) {
			String name = getAscii(data);
			String attr = getUnicode(data);
			attributes.put(name, attr);
		}
		getInt(data);
	}
	
	public ByteBuffer encode() {
		int size = 0;
		for (Entry <String, String> e : attributes.entrySet()) {
			size += 6 + e.getKey().length() + e.getValue().length();
		}
		ByteBuffer data = ByteBuffer.allocate(18 + size);
		addShort(data, 3);
		addInt  (data, CRC);
		addLong (data, objectId);
		addShort(data, 0);
		addInt  (data, attributes.size());
		for (Entry <String, String> e : attributes.entrySet()) {
			addAscii(data, e.getKey());
			addUnicode(data, e.getValue());
		}
		addInt  (data, 0);
		return data;
	}
	
}

