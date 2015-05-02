package network.packets.swg.zone.server_ui;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import network.packets.swg.SWGPacket;

public class SuiEventNotification extends SWGPacket {
	
	public static final int CRC = 0x092D3564;
	
	private int windowId;
	private int eventId;
	private int updateCount;
	private List <String> dataStrings;
	
	public SuiEventNotification() { 
		dataStrings = new ArrayList<String>();
	}
	
	public SuiEventNotification(ByteBuffer data) {
		this();
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		windowId = getInt(data);
		eventId = getInt(data);
		int size = getInt(data);
		updateCount = getInt(data);
		for (int i = 0; i < size; i++) {
			dataStrings.add(getUnicode(data));
		}
	}
	
	public ByteBuffer encode() {
		return null;
	}
	
	public int getWindowId() { return this.windowId; }
	public List<String> getDataStrings() { return this.dataStrings; }
	public int getEventId() { return this.eventId; }
	public int getUpdateCount() { return this.updateCount; }
}
