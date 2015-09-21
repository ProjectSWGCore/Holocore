package resources;

import resources.common.CRC;

public enum PvpFaction {

	NEUTRAL(0),
	IMPERIAL(CRC.getCrc("imperial")),
	REBEL(CRC.getCrc("rebel"));
	
	private int crc;
	
	PvpFaction(int crc) {
		this.crc = crc;
	}
	
	public int getCrc() {
		return crc;
	}
	
}