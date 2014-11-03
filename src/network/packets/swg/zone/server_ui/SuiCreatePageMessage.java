package network.packets.swg.zone.server_ui;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import network.packets.swg.SWGPacket;

public class SuiCreatePageMessage extends SWGPacket {
	
	public static final int CRC = 0xD44B7259;
	
	private int windowId;
	private String scriptName;
	private List <SuiWindowComponent> components;
	private long objectId;
	private float maxDistance;
	
	public SuiCreatePageMessage() {
		this(0, "", new ArrayList<SuiWindowComponent>(), 0, 0);
	}
	
	public SuiCreatePageMessage(int windowId, String scriptName, List <SuiWindowComponent> components, long objectId, float maxDistance) {
		this.windowId = windowId;
		this.scriptName = scriptName;
		this.components = components;
		this.objectId = objectId;
		this.maxDistance = maxDistance;
	}
	
	public SuiCreatePageMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		windowId = getInt(data);
		scriptName = getAscii(data);
		int compSize = getInt(data);
		components.clear();
		for (int compI = 0; compI < compSize; compI++) {
			SuiWindowComponent comp = new SuiWindowComponent();
			comp.setType(getByte(data));
			int wideSize = getInt(data);
			for (int wideI = 0; wideI < wideSize; wideI++)
				comp.getWideParams().add(getUnicode(data));
			int narrowSize = getInt(data);
			for (int narrowI = 0; narrowI < narrowSize; narrowI++)
				comp.getNarrowParams().add(getAscii(data));
			components.add(comp);
		}
		objectId = getLong(data);
		maxDistance = getFloat(data);
		getLong(data);
		getInt(data);
	}
	
	public ByteBuffer encode() {
		int extraSize = 0;
		for (SuiWindowComponent comp : components) {
			extraSize += 9;
			for (String s : comp.getWideParams())
				extraSize += 4 + s.length()*2;
			for (String s : comp.getNarrowParams())
				extraSize += 2 + s.length();
		}
		ByteBuffer data = ByteBuffer.allocate(40+scriptName.length()+extraSize);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt  (data, windowId);
		addAscii(data, scriptName);
		addInt  (data, components.size());
		for (SuiWindowComponent comp : components) {
			addByte(data, comp.getType());
			addInt (data, comp.getWideParams().size());
			for (String wide : comp.getWideParams())
				addUnicode(data, wide);
			addInt (data, comp.getNarrowParams().size());
			for (String narrow : comp.getNarrowParams())
				addAscii(data, narrow);
		}
		addLong (data, objectId);
		addFloat(data, maxDistance);
		addLong (data, 0);
		addInt  (data, 0);
		return data;
	}
	
	public static class SuiWindowComponent {
		
		private byte type; 
		private List <String> narrowParams = new ArrayList<String>();
		private List <String> wideParams = new ArrayList<String>();
		
		public byte getType() {
			return type;
		}
		public void setType(byte type) {
			this.type = type;
		}
		public List <String> getNarrowParams() {
			return narrowParams;
		}
		public void setNarrowParams(List <String> narrowParams) {
			this.narrowParams = narrowParams;
		}
		public List <String> getWideParams() {
			return wideParams;
		}
		public void setWideParams(List <String> wideParams) {
			this.wideParams = wideParams;
		}
		public void addNarrowParam(String param) {
			narrowParams.add(param);
		}
		public void addWideParam(String param) {
			wideParams.add(param);
		}
		
	}
	
}
