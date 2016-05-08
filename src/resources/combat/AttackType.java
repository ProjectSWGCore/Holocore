package resources.combat;

public enum AttackType {
	CONE								(0),
	SINGLE_TARGET						(1),
	AREA								(2),
	TARGET_AREA							(3),
	DUAL_WIELD							(4),
	RAMPAGE								(5),
	RANDOM_HATE_TARGET					(6),
	RANDOM_HATE_TARGET_CONE				(7),
	RANDOM_HATE_TARGET_CONE_TERMINUS	(8),
	HATE_LIST							(9),
	RANDOM_HATE_MULTI					(10),
	AREA_PROGRESSIVE					(11),
	SPLIT_DAMAGE_TARGET_AREA			(12),
	DISTANCE_FARTHEST					(13);
	
	private static final AttackType [] VALUES = values();
	
	private int num;
	
	AttackType(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static AttackType getAttackType(int num) {
		if (num < 0 || num >= VALUES.length)
			return AttackType.SINGLE_TARGET;
		return VALUES[num];
	}
}
