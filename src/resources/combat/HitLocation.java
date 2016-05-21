package resources.combat;

public enum HitLocation {
	HIT_LOCATION_BODY			(0),
	HIT_LOCATION_HEAD			(1),
	HIT_LOCATION_R_ARM			(2),
	HIT_LOCATION_L_ARM			(3),
	HIT_LOCATION_R_LEG			(4),
	HIT_LOCATION_L_LEG			(5),
	HIT_LOCATION_NUM_LOCATIONS	(6);
	
	private static final HitLocation [] VALUES = values();
	
	private int num;
	
	HitLocation(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static HitLocation getHitLocation(int num) {
		if (num < 0 || num >= VALUES.length)
			return HIT_LOCATION_BODY;
		return VALUES[num];
	}
}
