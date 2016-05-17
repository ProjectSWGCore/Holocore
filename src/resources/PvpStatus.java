package resources;

import java.util.HashMap;
import java.util.Map;

public enum PvpStatus {

	ONLEAVE(0),
	COMBATANT(1),
	SPECIALFORCES(2);
	
	private static final Map<Integer, PvpStatus> CRC_LOOKUP;
	
	static {
		CRC_LOOKUP = new HashMap<>();
		
		for(PvpStatus pvpStatus : values()) {
			CRC_LOOKUP.put(pvpStatus.getValue(), pvpStatus);
		}
	}
	
	private int value;
	
	PvpStatus(int crc) {
		this.value = crc;
	}
	
	public int getValue() {
		return value;
	}
	
	public static PvpStatus getStatusForValue(int crc) {
		return CRC_LOOKUP.get(crc);
	}
	
}