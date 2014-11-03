package network.packets.swg.zone.server_ui;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import network.packets.swg.SWGPacket;

public class SuiEventNotification extends SWGPacket {
	
	public static final int CRC = 0x092D3564;
	
	private int windowId;
	private boolean action;
	private List <String> dataStrings;
	
	public SuiEventNotification() {
		this(0, false, new String[0]);
	}
	
	public SuiEventNotification(int windowId, boolean action, String [] strings) {
		this(windowId, action, new ArrayList<String>(Arrays.asList(strings)));
	}
	
	public SuiEventNotification(int windowId, boolean action, List <String> strings) {
		this.windowId = windowId;
		this.action = action;
		this.dataStrings = strings;
	}
	
	public SuiEventNotification(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		windowId = getInt(data);
		action = getInt(data) != 0;
		int size = getInt(data);
		size = getInt(data); // loljk, the size is this
		for (int i = 0; i < size; i++) {
			dataStrings.add(getUnicode(data));
		}
	}
	
	public ByteBuffer encode() {
		int extraSize = 0;
		for (String s : dataStrings)
			extraSize += 4 + s.length()*2;
		ByteBuffer data = ByteBuffer.allocate(22+extraSize);
		addShort(data, 2);
		addInt  (data, CRC);
		addInt  (data, windowId);
		addInt  (data, action?0xFFFFFFFF : 0);
		addInt  (data, dataStrings.size());
		addInt  (data, dataStrings.size());
		for (String s : dataStrings)
			addUnicode(data, s);
		return data;
	}
	
}
