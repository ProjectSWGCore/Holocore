package resources.combat;

import java.util.EnumSet;
import java.util.Set;

public enum TrailLocation {
	LEFT_FOOT	(0x01),
	RIGHT_FOOT	(0x02),
	LEFT_HAND	(0x04),
	RIGHT_HAND	(0x08),
	WEAPON		(0x10);
	
	private static final TrailLocation [] VALUES = values();
	
	private int num;
	
	TrailLocation(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static TrailLocation getTrailLocation(int num) {
		for (TrailLocation type : VALUES) {
			if ((num & type.getNum()) != 0)
				return type;
		}
		return WEAPON;
	}
	
	public static Set<TrailLocation> getTrailLocations(int num) {
		Set<TrailLocation> types = EnumSet.noneOf(TrailLocation.class);
		for (TrailLocation type : VALUES) {
			if ((num & type.getNum()) != 0)
				types.add(type);
		}
		return types;
	}
	
}
