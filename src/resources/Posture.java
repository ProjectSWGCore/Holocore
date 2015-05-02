package resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum Posture {
	UPRIGHT			(0x00),
	CROUCHED		(0x01),
	PRONE			(0x02),
	SNEAKING		(0x03),
	BLOCKING		(0x04),
	CLIMBING		(0x05),
	FLYING			(0x06),
	LYING_DOWN		(0x07),
	SITTING			(0x08),
	SKILL_ANIMATING	(0x09),
	DRIVING_VEHICLE	(0x0A),
	RIDING_CREATURE	(0x0B),
	KNOCKED_DOWN	(0x0C),
	INCAPACITATED	(0x0D),
	DEAD			(0x0E),
	INVALID			(0x0E);
	
	private static final Map <Byte, Posture> POSTURE_MAP = new ConcurrentHashMap<Byte, Posture>();
	private byte id;
	
	static {
		for (Posture p : values())
			POSTURE_MAP.put(p.getId(), p);
	}
	
	Posture(int id) {
		this.id = (byte)id;
	}
	
	public byte getId() { return id; }
	
	public static final Posture getFromId(byte id) {
		Posture p = POSTURE_MAP.get(id);
		if (p == null)
			return INVALID;
		return p;
	}
	
}
