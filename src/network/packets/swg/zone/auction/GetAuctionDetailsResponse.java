package network.packets.swg.zone.auction;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import network.packets.swg.SWGPacket;

public class GetAuctionDetailsResponse extends SWGPacket {
	
	public static final int CRC = 0xFE0E644B;
	
	private long itemId;
	private Map <String, String> properties;
	private String itemName;
	
	public GetAuctionDetailsResponse() {
		this(0, new HashMap<String, String>(), "");
	}
	
	public GetAuctionDetailsResponse(long itemId, Map <String, String> properties, String itemName) {
		this.itemId = itemId;
		this.properties = properties;
		this.itemName = itemName;
	}
	
	public GetAuctionDetailsResponse(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		itemId = getLong(data);
		getInt(data);
		int count = getInt(data);
		for (int i = 0; i < count; i++) {
			String key = getAscii(data);
			String val = getUnicode(data);
			properties.put(key, val);
		}
		itemName = getAscii(data);
		getShort(data); // 0
	}
	
	public ByteBuffer encode() {
		int strSize = 0;
		for (Entry <String, String> e : properties.entrySet())
			strSize += 6 + e.getKey().length() + e.getValue().length()*2;
		ByteBuffer data = ByteBuffer.allocate(18 + strSize);
		addShort(data, 9);
		addInt  (data, CRC);
		addLong (data, itemId);
		addInt  (data, properties.size());
		for (Entry <String, String> e : properties.entrySet()) {
			addAscii(data, e.getKey());
			addUnicode(data, e.getValue());
		}
		addAscii(data, itemName);
		addShort(data, 0);
		return data;
	}

}
