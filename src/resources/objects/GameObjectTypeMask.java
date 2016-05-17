package resources.objects;

import java.util.Arrays;

public enum GameObjectTypeMask {
	GOTM_NONE					(0x00000000),
	GOTM_ARMOR					(0x00000100),
	GOTM_BUILDING				(0x00000200),
	GOTM_CREATURE				(0x00000400),
	GOTM_DATA					(0x00000800),
	GOTM_INSTALLATION			(0x00001000),
	GOTM_CHRONICLES				(0x00001100),
	GOTM_MISC					(0x00002000),
	GOTM_TERMINAL				(0x00004000),
	GOTM_TOOL					(0x00008000),
	GOTM_VEHICLE				(0x00010000),
	GOTM_WEAPON					(0x00020000),
	GOTM_COMPONENT				(0x00040000),
	GOTM_POWERUP_WEAPON			(0x00100000),
	GOTM_JEWELRY				(0x00200000),
	GOTM_RESOURCE_CONTAINER		(0x00400000),
	GOTM_DEED					(0x00800000),
	GOTM_CLOTHING				(0x01000000),
	GOTM_SHIP					(0x20000000),
	GOTM_CYBERNETIC				(0x20000100),
	GOTM_SHIP_COMPONENT			(0x40000000);
	
	private static final GameObjectTypeMask [] MASKS;
	private static final long [] MASK_LIST;
	
	static {
		MASKS = values();
		MASK_LIST = new long[MASKS.length];
		for (int i = 0; i < MASKS.length; i++) {
			MASK_LIST[i] = MASKS[i].getMask() & 0xFFFFFFFFL;
		}
	}
	
	private int mask;
	
	GameObjectTypeMask(int mask) {
		this.mask = mask;
	}
	
	public int getMask() {
		return mask;
	}
	
	public static GameObjectTypeMask getFromMask(long mask) {
		int ind = Arrays.binarySearch(MASK_LIST, mask & 0xFFFFFFFFL);
		if (ind < 0)
			return GOTM_NONE;
		return MASKS[ind];
	}
}
