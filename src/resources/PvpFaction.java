package resources;

import java.util.HashMap;
import java.util.Map;

import resources.common.CRC;

public enum PvpFaction {

	NEUTRAL(0),
	IMPERIAL(CRC.getCrc("imperial")),
	REBEL(CRC.getCrc("rebel"));
	
	private static final Map<Integer, PvpFaction> CRC_LOOKUP;
	
	static {
		CRC_LOOKUP = new HashMap<>();
		
		for(PvpFaction pvpFaction : values()) {
			CRC_LOOKUP.put(pvpFaction.getCrc(), pvpFaction);
		}
	}
	
	private int crc;
	
	PvpFaction(int crc) {
		this.crc = crc;
	}
	
	public int getCrc() {
		return crc;
	}
	
	public static PvpFaction getFactionForCrc(int crc) {
		return CRC_LOOKUP.get(crc);
	}
	
}
